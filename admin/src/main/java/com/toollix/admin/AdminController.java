package com.toollix.admin;

import com.toollix.nfc.repo.NfcCardRepository;
import com.toollix.orders.repo.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final NfcCardRepository nfcRepo;
    private final OrderRepository orderRepo;

    public AdminController(NfcCardRepository nfcRepo, OrderRepository orderRepo) {
        this.nfcRepo = nfcRepo;
        this.orderRepo = orderRepo;
    }

    @GetMapping("/nfc/inventory")
    public ResponseEntity<?> nfcInventory() {
        return ResponseEntity.ok(nfcRepo.findAll());
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders() {
        return ResponseEntity.ok(orderRepo.findAll());
    }
}
