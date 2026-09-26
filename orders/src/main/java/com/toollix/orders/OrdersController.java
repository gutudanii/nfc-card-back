package com.toollix.orders;

import com.toollix.orders.model.Order;
import com.toollix.orders.repo.OrderRepository;
import com.toollix.orders.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrdersController {

    public final OrderRepository repo;
    private final OrderService orderService;

    public OrdersController(OrderRepository repo, OrderService orderService) { this.repo = repo; this.orderService = orderService; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Order o) {
        var saved = orderService.createOrder(o);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
