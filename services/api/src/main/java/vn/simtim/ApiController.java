package vn.simtim;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final StoreService store;

  public ApiController(StoreService store) {
    this.store = store;
  }

  @GetMapping("/health")
  public Object health() {
    store.get(Model.StoreLock.class, "STORE01");
    return Map.of("status", "UP", "database", "PostgreSQL");
  }

  @GetMapping("/csrf")
  public Object csrf(CsrfToken csrf) {
    return Map.of("token", csrf.getToken(), "headerName", csrf.getHeaderName());
  }

  @GetMapping("/me")
  public Object me(Authentication auth) {
    var a = store.get(Model.Account.class, auth.getName());
    return Map.of("id", a.id, "name", a.name, "role", a.role);
  }

  @GetMapping("/state")
  public Object state(Authentication auth) {
    var a = store.get(Model.Account.class, auth.getName());
    return store.state(a.id, a.role);
  }

  @GetMapping("/products")
  public Object products(@RequestParam(defaultValue = "") String query) {
    return store.all(Model.Product.class, "Product").stream()
        .filter(
            p ->
                p.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))
                    || p.barcode.contains(query))
        .toList();
  }

  @GetMapping("/categories")
  public Object categories() {
    return store.all(Model.Category.class, "Category");
  }

  @GetMapping("/suppliers")
  public Object suppliers() {
    return store.all(Model.Supplier.class, "Supplier");
  }

  @PostMapping({"/products", "/categories", "/suppliers"})
  @PreAuthorize("hasAnyRole('admin','manager')")
  public Object create(
      jakarta.servlet.http.HttpServletRequest req, @RequestBody Map<String, String> body) {
    return store.catalog(req.getRequestURI().substring(5), null, body, false);
  }

  @PutMapping("/{kind:products|categories|suppliers}/{id}")
  @PreAuthorize("hasAnyRole('admin','manager')")
  public Object edit(
      @PathVariable String kind, @PathVariable String id, @RequestBody Map<String, String> body) {
    return store.catalog(kind, id, body, false);
  }

  @DeleteMapping("/{kind:products|categories|suppliers}/{id}")
  @PreAuthorize("hasAnyRole('admin','manager')")
  public Object delete(@PathVariable String kind, @PathVariable String id) {
    return store.catalog(kind, id, Map.of(), true);
  }

  public record Line(@NotBlank String id, @Min(1) @Max(10000) int quantity) {}

  public record Sale(
      @NotEmpty @Size(max = 100) List<@Valid Line> lines,
      @NotBlank String method,
      @Min(0) @Max(1000000000000L) long tendered) {}

  @PostMapping("/invoices")
  @PreAuthorize("hasAnyRole('admin','sales')")
  public Object sale(
      Authentication auth,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody Sale body) {
    return store.sell(auth.getName(), key, body);
  }

  public record Receiving(
      @NotBlank String supplier,
      @NotBlank String product,
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,60}") String lot,
      @NotNull LocalDate expiry,
      @Min(1) @Max(1000000) int delivered,
      @Min(1) @Max(1000000) int accepted,
      @NotNull @Size(max = 500) String note) {}

  @PostMapping("/receipts")
  @PreAuthorize("hasAnyRole('admin','stock')")
  public Object receive(
      Authentication auth,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody Receiving body) {
    return store.receive(auth.getName(), key, body);
  }

  public record CountRequest(
      @NotBlank String batchId,
      @Min(0) int expected,
      @Min(0) @Max(1000000) int actual,
      @Min(0) long baseVersion,
      @NotNull @Size(max = 500) String note) {}

  @PostMapping("/counts")
  @PreAuthorize("hasAnyRole('admin','manager')")
  public Object count(
      Authentication auth,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody CountRequest body) {
    return store.count(auth.getName(), key, body);
  }

  @PostMapping("/counts/{id}/approve")
  @PreAuthorize("hasAnyRole('admin','manager')")
  public Object approve(Authentication auth, @PathVariable String id) {
    return store.approve(auth.getName(), id);
  }

  public record Discount(
      @NotBlank String productId, @Min(1) @Max(90) int percent, @NotNull LocalDate end) {}

  @PostMapping("/promotions")
  @PreAuthorize("hasAnyRole('admin','manager')")
  public Object promotion(@Valid @RequestBody Discount body) {
    return store.promote(body);
  }

  public record Chapter(@Min(0) @Max(6) int chapter) {}

  @PostMapping("/training/sessions")
  public Object start(Authentication auth, @Valid @RequestBody Chapter body) {
    return store.startTraining(auth.getName(), body.chapter());
  }

  public record Visit(@NotBlank String station) {}

  public record Orientation(
      @NotBlank String welcome, @NotBlank String sensitive, @NotBlank String delivery) {}

  @PostMapping("/training/sessions/{id}/orientation")
  public Object orientation(
      Authentication auth, @PathVariable String id, @Valid @RequestBody Orientation body) {
    return store.orientation(auth.getName(), id, body);
  }

  @PostMapping("/training/sessions/{id}/events")
  public Object event(
      Authentication auth, @PathVariable String id, @Valid @RequestBody Visit body) {
    return store.trainingEvent(auth.getName(), id, body.station(), false);
  }

  @PostMapping("/training/sessions/{id}/complete")
  public Object complete(Authentication auth, @PathVariable String id) {
    return store.trainingEvent(auth.getName(), id, "", true);
  }

  @GetMapping("/training/sessions")
  public Object sessions(Authentication auth) {
    return store.all(Model.TrainingSession.class, "TrainingSession").stream()
        .filter(s -> s.actor.equals(auth.getName()))
        .toList();
  }
}
