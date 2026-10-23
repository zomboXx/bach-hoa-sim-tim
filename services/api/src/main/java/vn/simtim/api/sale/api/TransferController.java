package vn.simtim.api.sale.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.sale.application.*;
import vn.simtim.api.sale.domain.TransferOrder;
import vn.simtim.api.sale.domain.TransferRepository;

@RestController
@RequestMapping("/api/v1/sales/transfers")
public class TransferController {
    private final TransferService service;
    private final TransferSettings settings;
    public TransferController(TransferService service,TransferSettings settings) { this.service=service; this.settings=settings; }
    @GetMapping("/config")
    public ResponseEntity<TransferSettings.Config> config(Authentication auth) {
        return noStore(settings.config(PosCartController.scope(auth)));
    }
    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Create request,Authentication auth) {
        return noStore(Response.from(service.create(PosCartController.scope(auth),request.operationId(),request.cartVersion(),request.expectedTotal())));
    }
    @GetMapping
    public ResponseEntity<History> history(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,Authentication auth) {
        if (page<0 || page>100000 || size<1 || size>100) throw new vn.simtim.api.sale.domain.SaleValidationException("Phân trang không hợp lệ.");
        var actor=principal(auth);
        return noStore(new History(service.list(actor,page,size).stream().map(Response::from).toList(),service.summary(actor),page,size));
    }
    @GetMapping("/{id}")
    public ResponseEntity<Response> get(@PathVariable UUID id,Authentication auth) { return noStore(Response.from(service.get(id,principal(auth)))); }
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Response> confirm(@PathVariable UUID id,@Valid @RequestBody Confirm request,Authentication auth) {
        return noStore(Response.from(service.confirm(id,principal(auth),request.receivedAmount(),request.bankReference(),request.checkedBankReceipt())));
    }
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Response> cancel(@PathVariable UUID id,@Valid @RequestBody Cancel request,Authentication auth) {
        return noStore(Response.from(service.cancel(id,principal(auth),request.reason())));
    }
    @PostMapping("/{id}/confirm-manual")
    public ResponseEntity<Response> confirmManual(@PathVariable UUID id,@RequestBody ManualConfirm request,Authentication auth) {
        return noStore(Response.from(service.confirmEmployee(id,principal(auth),request.confirmed())));
    }
    private static SessionPrincipal principal(Authentication auth) { return (SessionPrincipal)auth.getPrincipal(); }
    private static <T> ResponseEntity<T> noStore(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
    public record Create(@NotNull UUID operationId,@Min(0) long cartVersion,@Positive long expectedTotal) {}
    public record Confirm(@Min(1) long receivedAmount,@NotBlank @Size(max=100) String bankReference,boolean checkedBankReceipt) {}
    public record Cancel(@NotBlank @Size(max=500) String reason) {}
    public record ManualConfirm(boolean confirmed) {}
    public record History(List<Response> items,TransferRepository.Summary summary,int page,int size) {}
    public record Response(UUID id,UUID invoiceId,UUID ownerId,String reference,long amount,String status,
            String bankBin,String accountNumber,String accountName,Instant createdAt,Instant expiresAt,Instant resolvedAt,
            UUID resolvedBy,String bankReference,String resolutionNote,String qrPayload,String confirmationMethod) {
        static Response from(TransferOrder o) {
            return new Response(o.id(),o.invoiceId(),o.ownerId(),o.reference(),o.amount(),o.status(),o.bankBin(),
                    o.accountNumber(),o.accountName(),o.createdAt(),o.expiresAt(),o.resolvedAt(),o.resolvedBy(),o.bankReference(),
                    o.resolutionNote(),o.pending()?VietQr.encode(o.bankBin(),o.accountNumber(),o.amount(),o.reference()):null,
                    o.confirmationMethod());
        }
    }
}
