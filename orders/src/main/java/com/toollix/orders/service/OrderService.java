package com.toollix.orders.service;

import com.toollix.orders.model.Order;
import com.toollix.orders.repo.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository repo;
    private final com.toollix.common.audit.AuditService auditService;

    public OrderService(OrderRepository repo, com.toollix.common.audit.AuditService auditService) { this.repo = repo; this.auditService = auditService; }

    public Order createOrder(Order o) { o.setStatus("PENDING"); var saved = repo.save(o); try { auditService.record("order.create", "system", "orderId="+saved.getId()); } catch (Exception ignored) {} return saved; }
}
