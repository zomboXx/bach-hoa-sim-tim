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
 *   POST   /api/v1/sales/quote           — preview giá, không ghi DB (quyền sales.read)
 *   POST   /api/v1/sales/invoices        — checkout CASH, tạo hóa đơn và trừ tồn (quyền sales.write)
 *   GET    /api/v1/sales/invoices        — danh sách hóa đơn của cửa hàng (quyền sales.read)
 *   GET    /api/v1/sales/invoices/{id}   — chi tiết hóa đơn (quyền sales.read)
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
                               @Valid @RequestBody QuoteRequest req) {
        var items = req.items().stream()
                .map(i -> new CheckoutItem(i.productId(), i.quantity()))
                .toList();
        return QuoteResponse.from(service.quote(orgId, req.storeId(), items));
    }

    /** Tạo hóa đơn CASH và trừ tồn nguyên tử. soldBy = session user. Yêu cầu quyền sales.write. */
    @PostMapping("/invoices")
    public ResponseEntity<InvoiceResponse> checkout(
            @RequestHeader("X-Organization-Id") UUID orgId,
            @Valid @RequestBody CheckoutRequest req,
            Authentication auth) {
        UUID soldBy = ((SessionPrincipal) auth.getPrincipal()).userId();
        var items = req.items().stream()
                .map(i -> new CheckoutItem(i.productId(), i.quantity()))
                .toList();
        var invoice = service.checkout(orgId, req.storeId(), soldBy, items, req.cashAmount());
        return ResponseEntity
                .created(URI.create("/api/v1/sales/invoices/" + invoice.id()))
                .body(InvoiceResponse.from(invoice));
    }

    /** Danh sách hóa đơn của một cửa hàng. Yêu cầu quyền sales.read. */
    @GetMapping("/invoices")
    public List<InvoiceResponse> list(@RequestHeader("X-Organization-Id") UUID orgId,
                                      @RequestParam UUID storeId) {
        return service.listInvoices(orgId, storeId).stream().map(InvoiceResponse::from).toList();
    }

    /** Chi tiết hóa đơn. Yêu cầu quyền sales.read. */
    @GetMapping("/invoices/{id}")
    public InvoiceResponse get(@RequestHeader("X-Organization-Id") UUID orgId,
                               @PathVariable UUID id) {
        return InvoiceResponse.from(service.getInvoice(orgId, id));
    }
}
