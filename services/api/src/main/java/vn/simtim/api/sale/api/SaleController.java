package vn.simtim.api.sale.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.sale.application.CheckoutItem;
import vn.simtim.api.sale.application.SaleService;

/**
 * HTTP adapter cho SAL-01: Quote, Checkout CASH, xem hóa đơn theo wire contract /api/v1/sales/...
 *
 * Endpoints:
 *   POST   /api/v1/sales/quote (alias: /api/v1/sales/invoices/quote) — preview giá, không ghi DB (quyền sales.read)
 *   POST   /api/v1/sales/checkout (alias: /api/v1/sales/invoices)     — checkout CASH, tạo hóa đơn và trừ tồn (quyền sales.write)
 *   GET    /api/v1/sales/invoices                                    — danh sách hóa đơn của cửa hàng (quyền sales.read)
 *   GET    /api/v1/sales/invoices/{id}                               — chi tiết hóa đơn (quyền sales.read)
 */
@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    /** Preview giá trước khi checkout. Yêu cầu quyền sales.read. */
    @PostMapping({"/quote", "/invoices/quote"})
    public QuoteResponse quote(@RequestHeader("X-Organization-Id") UUID orgId,
                               @Valid @RequestBody QuoteRequest req,
                               Authentication auth) {
        SessionPrincipal principal = (SessionPrincipal) auth.getPrincipal();
        UUID storeId = req.storeId() != null ? req.storeId() : principal.storeId();
        if (!storeId.equals(principal.storeId())) {
            throw new vn.simtim.api.sale.domain.SaleValidationException("Cửa hàng không thuộc phiên làm việc");
        }
        var items = req.items().stream()
                .map(i -> new CheckoutItem(i.productId(), i.quantity()))
                .toList();
        return QuoteResponse.from(service.quote(orgId, storeId, items));
    }

    /** Tạo hóa đơn CASH và trừ tồn nguyên tử. soldBy = session user. Yêu cầu quyền sales.write. */
    @PostMapping({"/checkout", "/invoices"})
    public ResponseEntity<InvoiceResponse> checkout(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @Valid @RequestBody CheckoutRequest req,
            Authentication auth) {
        SessionPrincipal principal = (SessionPrincipal) auth.getPrincipal();
        UUID storeId = req.storeId() != null ? req.storeId() : principal.storeId();
        if (!storeId.equals(principal.storeId())) {
            throw new vn.simtim.api.sale.domain.SaleValidationException("Cửa hàng không thuộc phiên làm việc");
        }
        UUID soldBy = principal.userId();
        var items = req.items().stream()
                .map(i -> new CheckoutItem(i.productId(), i.quantity()))
                .toList();
        var invoice = service.checkout(orgId, storeId, soldBy, items, req.cashAmount());
        return ResponseEntity
                .created(URI.create("/api/v1/sales/invoices/" + invoice.id()))
                .body(InvoiceResponse.from(invoice));
    }

    /** Danh sách hóa đơn của một cửa hàng. Yêu cầu quyền sales.read. */
    @GetMapping("/invoices")
    public List<InvoiceResponse> list(@RequestHeader("X-Organization-Id") UUID orgId,
                                      @RequestParam(required = false) UUID storeId,
                                      Authentication auth) {
        SessionPrincipal principal = (SessionPrincipal) auth.getPrincipal();
        UUID effectiveStoreId = storeId != null ? storeId : principal.storeId();
        if (!effectiveStoreId.equals(principal.storeId())) {
            throw new vn.simtim.api.sale.domain.SaleValidationException("Cửa hàng không thuộc phiên làm việc");
        }
        return service.listInvoices(orgId, effectiveStoreId).stream().map(InvoiceResponse::from).toList();
    }

    /** Chi tiết hóa đơn. Yêu cầu quyền sales.read trong phạm vi store. */
    @GetMapping("/invoices/{id}")
    public InvoiceResponse get(@RequestHeader("X-Organization-Id") UUID orgId,
                               @PathVariable UUID id,
                               Authentication auth) {
        SessionPrincipal principal = (SessionPrincipal) auth.getPrincipal();
        return InvoiceResponse.from(service.getInvoice(orgId, principal.storeId(), id));
    }
}
