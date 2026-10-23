package vn.simtim.api.sale.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.simtim.api.catalog.domain.ProductRepository;
import vn.simtim.api.catalog.domain.UnitRepository;
import vn.simtim.api.sale.domain.*;

@Service
@Transactional
public class PosCartService {
    private final PosCartRepository carts;
    private final ProductRepository products;
    private final UnitRepository units;
    private final SaleService sales;
    private final TransferService transfers;

    public PosCartService(PosCartRepository carts, ProductRepository products, UnitRepository units, SaleService sales, TransferService transfers) {
        this.carts = carts;
        this.products = products;
        this.units = units;
        this.sales = sales;
        this.transfers = transfers;
    }

    public PosCart read(PosCart.Scope scope) {
        transfers.expireCart(scope);
        return carts.read(scope, false);
    }

    public void assertUnlocked(PosCart.Scope scope) { requireUnlocked(read(scope)); }

    private static void requireUnlocked(PosCart cart) {
        if (cart.pendingTransferId()!=null) throw new SaleConflictException("TRANSFER_PENDING",
                "Giỏ đang chờ chuyển khoản; hãy xác nhận hoặc hủy giao dịch trước.");
    }

    public PosCart adjust(PosCart.Scope scope, UUID operationId, long generation, UUID productId, BigDecimal delta) {
        if (operationId == null || productId == null || delta == null || delta.signum() == 0) {
            throw new SaleValidationException("Thao tác giỏ hàng không hợp lệ");
        }
        PosCart cart = carts.read(scope, true);
        String hash = hash("ADJUST|" + generation + "|" + productId + "|" + decimal(delta));
        var previous = carts.operation(cart.id(), operationId);
        if (previous.isPresent()) {
            requireSameHash(previous.get(), hash);
            return cart;
        }
        requireUnlocked(cart);
        if (generation != cart.generation()) {
            throw new SaleConflictException("CART_CHANGED", "Đơn cũ đã thanh toán trên thiết bị khác. Hãy kiểm tra giỏ mới.");
        }
        var product = products.findById(scope.organizationId(), productId)
                .filter(p -> p.isActive()).orElseThrow(() -> new SaleNotFoundException("Sản phẩm không đang bán"));
        var unit = units.findById(scope.organizationId(), product.baseUnitId())
                .orElseThrow(() -> new SaleNotFoundException("Không tìm thấy đơn vị sản phẩm"));
        if (delta.stripTrailingZeros().scale() > unit.precisionScale()) {
            throw new SaleValidationException("QUANTITY_PRECISION", "Số lượng không đúng độ chính xác của đơn vị");
        }
        BigDecimal current = cart.items().stream().filter(l -> l.productId().equals(productId))
                .map(PosCart.Line::quantity).findFirst().orElse(BigDecimal.ZERO);
        BigDecimal next = current.add(delta).max(BigDecimal.ZERO);
        if (next.compareTo(new BigDecimal("99999999999.999")) > 0) {
            throw new SaleValidationException("Số lượng quá lớn");
        }
        if (cart.items().size() >= 100 && current.signum() == 0 && next.signum() > 0) {
            throw new SaleValidationException("Giỏ hàng tối đa 100 sản phẩm");
        }
        carts.setQuantity(cart.id(), productId, next);
        carts.advance(cart.id(), false);
        carts.record(cart.id(), operationId, hash, null);
        return carts.read(scope, true);
    }

    public PosCart setQuantity(PosCart.Scope scope, UUID operationId, long generation, long expectedVersion,
                               UUID productId, BigDecimal quantity) {
        if (operationId==null || productId==null || quantity==null || quantity.signum()<0
                || quantity.compareTo(new BigDecimal("99999999999.999"))>0) {
            throw new SaleValidationException("Số lượng không hợp lệ");
        }
        var cart=carts.read(scope,true);
        String operationHash=hash("SET|"+generation+"|"+expectedVersion+"|"+productId+"|"+decimal(quantity));
        var previous=carts.operation(cart.id(),operationId);
        if (previous.isPresent()) {
            requireSameHash(previous.get(),operationHash);
            return cart;
        }
        requireUnlocked(cart);
        if (generation!=cart.generation() || expectedVersion!=cart.version())
            throw new SaleConflictException("CART_CHANGED","Giỏ đã thay đổi trên thiết bị khác. Hãy nhập lại số lượng.");
        var product=products.findById(scope.organizationId(),productId).filter(p->p.isActive())
                .orElseThrow(()->new SaleNotFoundException("Sản phẩm không đang bán"));
        var unit=units.findById(scope.organizationId(),product.baseUnitId())
                .orElseThrow(()->new SaleNotFoundException("Không tìm thấy đơn vị sản phẩm"));
        if (quantity.stripTrailingZeros().scale()>unit.precisionScale())
            throw new SaleValidationException("QUANTITY_PRECISION","Số lượng không đúng độ chính xác của đơn vị");
        boolean exists=cart.items().stream().anyMatch(l->l.productId().equals(productId));
        if (!exists && quantity.signum()>0 && cart.items().size()>=100)
            throw new SaleValidationException("Giỏ hàng tối đa 100 sản phẩm");
        carts.setQuantity(cart.id(),productId,quantity);
        carts.advance(cart.id(),false);
        carts.record(cart.id(),operationId,operationHash,null);
        return carts.read(scope,true);
    }

    public Invoice checkout(PosCart.Scope scope, long expectedVersion, List<CheckoutItem> requestedItems,
                            Long expectedTotal, long cashAmount, UUID operationId) {
        if (operationId == null || expectedVersion < 0 || expectedTotal == null) {
            throw new SaleValidationException("Shared checkout cần Idempotency-Key, phiên bản giỏ và tổng đã xác nhận");
        }
        PosCart cart = carts.read(scope, true);
        String hash = hash("CHECKOUT|" + expectedVersion + "|" + expectedTotal + "|" + cashAmount + "|" + itemsKey(requestedItems));
        var previous = carts.operation(cart.id(), operationId);
        if (previous.isPresent()) {
            requireSameHash(previous.get(), hash);
            if (previous.get().invoiceId() == null) {
                throw new SaleConflictException("IDEMPOTENCY_KEY_REUSED", "Mã này đã dùng cho thao tác sửa giỏ");
            }
            return sales.getInvoice(scope.organizationId(), scope.storeId(), previous.get().invoiceId(), scope.ownerId());
        }
        var items = cart.items().stream().map(l -> new CheckoutItem(l.productId(), l.quantity())).toList();
        requireUnlocked(cart);
        if (cart.version() != expectedVersion || !itemsKey(items).equals(itemsKey(requestedItems))) {
            throw new SaleConflictException("CART_CHANGED", "Giỏ đã thay đổi trên thiết bị khác. Hãy xác nhận lại báo giá.");
        }
        Invoice invoice = sales.checkout(scope.organizationId(), scope.storeId(), scope.ownerId(),
                items, expectedTotal, cashAmount, operationId);
        carts.advance(cart.id(), true);
        carts.record(cart.id(), operationId, hash, invoice.id());
        return invoice;
    }

    private static String itemsKey(List<CheckoutItem> items) {
        return items.stream().sorted(Comparator.comparing(i -> i.productId().toString()))
                .map(i -> i.productId() + ":" + decimal(i.quantity())).reduce("", (a, b) -> a + "|" + b);
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static void requireSameHash(PosCart.Operation operation, String hash) {
        if (!operation.payloadHash().equals(hash)) {
            throw new SaleConflictException("IDEMPOTENCY_KEY_REUSED", "Mã thao tác đã được dùng với dữ liệu khác");
        }
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
