package vn.simtim.api.sale.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.simtim.api.auth.domain.SessionPrincipal;
import vn.simtim.api.sale.application.PosCartService;
import vn.simtim.api.sale.domain.PosCart;

@RestController
@RequestMapping("/api/v1/sales/cart")
public class PosCartController {
    private final PosCartService carts;
    public PosCartController(PosCartService carts) { this.carts = carts; }

    @GetMapping
    public ResponseEntity<CartResponse> get(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch, Authentication auth) {
        var cart = carts.read(scope(auth));
        String tag = "\"" + cart.id() + ":" + cart.version() + "\"";
        if (tag.equals(ifNoneMatch)) return ResponseEntity.status(304).header("Cache-Control", "no-store").eTag(tag).build();
        return ResponseEntity.ok().header("Cache-Control", "no-store").eTag(tag).body(CartResponse.from(cart));
    }

    @PostMapping("/operations")
    public CartResponse adjust(@Valid @RequestBody Adjustment request, Authentication auth) {
        return CartResponse.from(carts.adjust(scope(auth), request.operationId(),
                request.generation(), request.productId(), request.delta()));
    }

    @PostMapping("/quantities")
    public CartResponse setQuantity(@Valid @RequestBody Quantity request, Authentication auth) {
        return CartResponse.from(carts.setQuantity(scope(auth),request.operationId(),request.generation(),
                request.expectedVersion(),request.productId(),request.quantity()));
    }

    public static PosCart.Scope scope(Authentication auth) {
        var principal = (SessionPrincipal) auth.getPrincipal();
        return new PosCart.Scope(principal.organizationId(), principal.storeId(), principal.userId());
    }

    public record Adjustment(@NotNull UUID operationId, @Min(0) long generation,
                             @NotNull UUID productId, @NotNull BigDecimal delta) {}
    public record Quantity(@NotNull UUID operationId,@Min(0) long generation,@Min(0) long expectedVersion,
                           @NotNull UUID productId,@NotNull BigDecimal quantity) {}
    public record CartResponse(UUID id, long version, long generation, List<Line> items, UUID pendingTransferId) {
        static CartResponse from(PosCart cart) {
            return new CartResponse(cart.id(), cart.version(), cart.generation(),
                    cart.items().stream().map(l -> new Line(l.productId(), l.productName(), l.quantity().toPlainString())).toList(),cart.pendingTransferId());
        }
    }
    public record Line(UUID productId, String productName, String quantity) {}
}
