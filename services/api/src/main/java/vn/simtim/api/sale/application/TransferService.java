package vn.simtim.api.sale.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.inventory.application.InventoryHoldPort;
import vn.simtim.api.inventory.application.StoreScope;
import vn.simtim.api.sale.domain.*;

@Service
@Transactional
public class TransferService {
    private final TransferRepository orders;
    private final PosCartRepository carts;
    private final SaleService sales;
    private final InventoryHoldPort holds;
    private final TransferSettings settings;
    private final Clock clock;

    public TransferService(TransferRepository orders,PosCartRepository carts,SaleService sales,
                           InventoryHoldPort holds,TransferSettings settings,Clock clock) {
        this.orders=orders; this.carts=carts; this.sales=sales; this.holds=holds; this.settings=settings; this.clock=clock;
    }
    public TransferOrder create(PosCart.Scope scope, UUID operation, long version, long expectedTotal) {
        var cart=carts.read(scope,true);
        var previous=orders.operation(scope,operation);
        if (previous.isPresent()) {
            var old=previous.get();
            if (old.cartVersion()!=version || old.amount()!=expectedTotal)
                throw new SaleConflictException("IDEMPOTENCY_KEY_REUSED","Mã tạo QR đã dùng với dữ liệu khác.");
            return expireLocked(old);
        }
        orders.pending(cart.id()).ifPresent(this::expireLocked);
        if (orders.pending(cart.id()).isPresent()) throw pending();
        cart=carts.read(scope,true);
        if (cart.version()!=version) throw new SaleConflictException("CART_CHANGED","Giỏ đã thay đổi. Hãy kiểm tra lại tổng tiền.");
        if (expectedTotal>9999999999999L) throw new SaleValidationException("Giá trị QR quá lớn.");
        var config=settings.require(scope);
        var items=cart.items().stream().map(l->new CheckoutItem(l.productId(),l.quantity())).toList();
        var invoice=sales.prepareTransfer(scope.organizationId(),scope.storeId(),scope.ownerId(),items,expectedTotal);
        var now=clock.instant();
        UUID id=UUID.randomUUID();
        var order=new TransferOrder(id,scope.organizationId(),scope.storeId(),scope.ownerId(),cart.id(),invoice.id(),
                operation,version,"ST"+id.toString().replace("-","").substring(0,20).toUpperCase(Locale.ROOT),
                invoice.grandTotal(),"PENDING",config.bankBin(),config.accountNumber(),config.accountName(),
                now,now.plus(config.expiresInMinutes(),ChronoUnit.MINUTES),null,null,null,null,null);
        orders.save(order);
        carts.advance(cart.id(),false);
        return order;
    }
    public TransferOrder get(UUID id, SessionPrincipal actor) {
        var order=authorized(id,actor);
        carts.read(order.scope(),true);
        return expireLocked(orders.find(id,true).orElseThrow());
    }
    public TransferOrder confirm(UUID id,SessionPrincipal actor,long receivedAmount,String bankReference,boolean checked) {
        var order=get(id,actor);
        String ref=bankReference==null?"":bankReference.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        if (!checked || receivedAmount!=order.amount() || !ref.matches("[A-Z0-9][A-Z0-9 ._/-]{2,99}"))
            throw new SaleValidationException("Kiểm tra tiền về, nhập đúng số tiền và mã giao dịch ngân hàng (3–100 ký tự).");
        if ("COMPLETED".equals(order.status())) {
            if (!ref.equals(order.bankReference())) throw new SaleConflictException("ALREADY_CONFIRMED","Giao dịch đã xác nhận với mã ngân hàng khác.");
            return order;
        }
        // Return the persisted terminal state, including lazy expiry, rather than rolling it back with an exception.
        if (!order.pending()) return order;
        if (orders.receiptUsed(order,ref)) throw new SaleConflictException("BANK_REFERENCE_REUSED","Mã giao dịch ngân hàng đã ghi nhận cho đơn khác.");
        holds.complete(new StoreScope(order.organizationId(),order.storeId()),orders.allocations(order,actor.userId()));
        orders.complete(order,actor.userId(),ref,clock.instant());
        carts.advance(order.cartId(),true);
        return orders.find(id,false).orElseThrow();
    }
    public TransferOrder confirmEmployee(UUID id,SessionPrincipal actor,boolean confirmed) {
        var order=get(id,actor);
        if (!confirmed) throw new SaleValidationException("Nhân viên cần xác nhận đã nhận đủ tiền.");
        if (!order.pending()) return order;
        holds.complete(new StoreScope(order.organizationId(),order.storeId()),orders.allocations(order,actor.userId()));
        orders.complete(order,actor.userId(),null,clock.instant());
        carts.advance(order.cartId(),true);
        return orders.find(id,false).orElseThrow();
    }
    public TransferOrder cancel(UUID id,SessionPrincipal actor,String note) {
        var order=get(id,actor);
        if (!order.pending()) return order;
        if (note==null || note.isBlank() || note.length()>500) throw new SaleValidationException("Nhập lý do hủy (tối đa 500 ký tự).");
        release(order,"CANCELLED",actor.userId(),note.trim());
        return orders.find(id,false).orElseThrow();
    }
    public void expireCart(PosCart.Scope scope) {
        var cart=carts.read(scope,true);
        orders.pending(cart.id()).ifPresent(this::expireLocked);
    }
    public void expire(UUID id) {
        var order=orders.find(id,false).orElse(null);
        if (order==null) return;
        carts.read(order.scope(),true);
        orders.find(id,true).ifPresent(this::expireLocked);
    }
    @Transactional(readOnly=true)
    public List<UUID> due() { return orders.expired(clock.instant()); }
    @Transactional(readOnly=true)
    public List<TransferOrder> list(SessionPrincipal actor,int page,int size) {
        return orders.list(actor.organizationId(),actor.storeId(),manager(actor)?null:actor.userId(),size,page*size);
    }
    @Transactional(readOnly=true)
    public TransferRepository.Summary summary(SessionPrincipal actor) {
        return orders.summary(actor.organizationId(),actor.storeId(),manager(actor)?null:actor.userId());
    }
    private TransferOrder authorized(UUID id,SessionPrincipal actor) {
        var order=orders.find(id,false).orElseThrow(()->new SaleNotFoundException("Không tìm thấy giao dịch."));
        if (!order.organizationId().equals(actor.organizationId()) || !order.storeId().equals(actor.storeId())
                || (!manager(actor) && !order.ownerId().equals(actor.userId())))
            throw new SaleNotFoundException("Không tìm thấy giao dịch.");
        return order;
    }
    private static boolean manager(SessionPrincipal actor) { return actor.roles().contains("ADMIN") || actor.roles().contains("MANAGER"); }
    private TransferOrder expireLocked(TransferOrder order) {
        if (order.pending() && !clock.instant().isBefore(order.expiresAt())) {
            release(order,"EXPIRED",null,"QR hết hạn; khoản tiền đến muộn cần đối chiếu riêng.");
            return orders.find(order.id(),false).orElseThrow();
        }
        return order;
    }
    private void release(TransferOrder order,String status,UUID actor,String note) {
        holds.release(new StoreScope(order.organizationId(),order.storeId()),orders.allocations(order,order.ownerId()));
        orders.release(order,status,actor,note,clock.instant());
        carts.advance(order.cartId(),false);
    }
    private static SaleConflictException pending() { return new SaleConflictException("TRANSFER_PENDING","Giỏ đang chờ chuyển khoản; hãy xử lý giao dịch trước."); }
}
