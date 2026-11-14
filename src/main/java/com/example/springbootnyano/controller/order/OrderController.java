package com.example.springbootnyano.controller.order;

import com.example.springbootnyano.entity.OrderEntity;
import com.example.springbootnyano.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/create")
    public OrderEntity createOrder(@RequestBody OrderEntity order) {
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderStatus("PENDING");
        if (order.getOrderDetails() != null) {
            order.getOrderDetails().forEach(detail -> detail.setOrder(order));
        }
        return orderRepository.save(order);
    }

    @GetMapping("/getAll")
    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }
}