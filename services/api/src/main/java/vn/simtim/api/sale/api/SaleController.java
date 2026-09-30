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
 * HTTP adapter cho SAL-01: Quote, Checkout CASH, xem hóa đơn.
 *
 * Endpoint:
 *   POST   /api/v1/invoices/quote   — preview giá, không ghi DB
 *   POST   /api/v1/invoices         — checkout CASH, tạo hóa đơn và trừ tồn
 *   GET    /api/v1/invoices         — danh sách hóa đơn của cửa hàng
 *   GET    /api/v1/invoices/{id}    — chi tiết hóa đơn
 */
@RestController
@RequestMapping("/api/v1/invoices")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    /** Preview giá trước khi checkout. Không cần quyền write. */
    @PostMapping("/quote")
    public QuoteResponse quote(@RequestHeader("X-Organization-Id") UUID orgId,
                               @Valid @RequestBody QuoteRequest req) {
        var items = req.items().stream()
                .map(i -> new CheckoutItem(i.productId(), i.quantity()))
                .toList();
        return QuoteResponse.from(service.quote(orgId, req.storeId(), items));
    }

    /** Tạo hóa đơn CASH và trừ tồn nguyên tử. soldBy = session user. */
    @PostMapping
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
                .created(URI.create("/api/v1/invoices/" + invoice.id()))
                .body(InvoiceResponse.from(invoice));
    }

    /** Danh sách hóa đơn của một cửa hàng. */
    @GetMapping
    public List<InvoiceResponse> list(@RequestHeader("X-Organization-Id") UUID orgId,
                                      @RequestParam UUID storeId) {
        return service.listInvoices(orgId, storeId).stream().map(InvoiceResponse::from).toList();
    }

    /** Chi tiết hóa đơn. */
    @GetMapping("/{id}")
    public InvoiceResponse get(@RequestHeader("X-Organization-Id") UUID orgId,
                               @PathVariable UUID id) {
        return InvoiceResponse.from(service.getInvoice(orgId, id));
    }
}
