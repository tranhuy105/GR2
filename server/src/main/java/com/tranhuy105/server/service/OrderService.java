package com.tranhuy105.server.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tranhuy105.server.dto.OrderCreateRequest;
import com.tranhuy105.server.dto.OrderDTO;
import com.tranhuy105.server.entity.DeliveryOrder;
import com.tranhuy105.server.entity.OrderStatus;
import com.tranhuy105.server.repository.DeliveryOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final DeliveryOrderRepository orderRepository;
    
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderDTO::fromEntity)
                .toList();
    }
    
    public OrderDTO getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(OrderDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }
    
    public List<OrderDTO> getPendingOrders() {
        return orderRepository.findByStatus(OrderStatus.PENDING).stream()
                .map(OrderDTO::fromEntity)
                .toList();
    }
    
    public List<OrderDTO> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(OrderDTO::fromEntity)
                .toList();
    }
    
    @Transactional
    public OrderDTO createOrder(OrderCreateRequest request) {
        DeliveryOrder order = DeliveryOrder.builder()
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .lat(request.getLat())
                .lng(request.getLng())
                .address(request.getAddress())
                .timeWindowStart(request.getTimeWindowStart())
                .timeWindowEnd(request.getTimeWindowEnd())
                .demand(request.getDemand())
                .serviceTime(request.getServiceTime())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        return OrderDTO.fromEntity(orderRepository.save(order));
    }
    
    @Transactional
    public OrderDTO updateOrder(Long id, OrderCreateRequest request) {
        DeliveryOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setLat(request.getLat());
        order.setLng(request.getLng());
        order.setAddress(request.getAddress());
        order.setTimeWindowStart(request.getTimeWindowStart());
        order.setTimeWindowEnd(request.getTimeWindowEnd());
        order.setDemand(request.getDemand());
        order.setServiceTime(request.getServiceTime());
        order.setNotes(request.getNotes());
        
        return OrderDTO.fromEntity(orderRepository.save(order));
    }
    
    @Transactional
    public OrderDTO updateStatus(Long id, OrderStatus status) {
        DeliveryOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        order.setStatus(status);
        if (status == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }
        return OrderDTO.fromEntity(orderRepository.save(order));
    }
    
    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
}
