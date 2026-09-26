package com.toollix.orders;

import com.toollix.orders.model.Order;
import com.toollix.orders.repo.OrderRepository;
import com.toollix.orders.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Orders controller.
 * POST /orders — place a new order (requires auth, auto-tags userId)
 * GET /orders/my — list logged-in user's orders
 * GET /orders/{id} — get a specific order
 */
@RestController
@RequestMapping("/orders")
public class OrdersController {

    private static final Logger log = LoggerFactory.getLogger(OrdersController.class);

    private final OrderRepository repo;
    private final OrderService orderService;

    public OrdersController(OrderRepository repo, OrderService orderService) {
        this.repo = repo;
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateOrderRequest req,
            @AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        Order o = new Order();
        o.setUserId(userId);
        o.setProductType(req.productType());
        o.setQuantity(req.quantity());
        o.setAmount(req.amount());
        o.setStatus("PENDING");
        var saved = orderService.createOrder(o);
        log.info("[ORDERS_CTRL] POST /orders — userId={} productType={} orderId={}", userId, req.productType(),
                saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/my")
    public ResponseEntity<?> myOrders(@AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        var orders = repo.findByUserIdOrderByCreatedAtDesc(userId);
        log.info("[ORDERS_CTRL] GET /orders/my — userId={} count={}", userId, orders.size());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") Long id,
            @AuthenticationPrincipal String principal) {
        Long userId = Long.parseLong(principal);
        return repo.findById(id)
                .filter(o -> o.getUserId() != null && o.getUserId().equals(userId))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Also allow /me/orders as alternative path
    // ─────────────────────────────────────────────────────────────────────────

    public record CreateOrderRequest(
            @NotBlank String productType,
            @NotNull Integer quantity,
            @NotNull Long amount) {
    }
}
