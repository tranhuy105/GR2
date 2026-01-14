package com.tranhuy105.server.repository;

import com.tranhuy105.server.entity.DeliveryOrder;
import com.tranhuy105.server.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {
    List<DeliveryOrder> findByStatus(OrderStatus status);
    List<DeliveryOrder> findByAssignedRouteId(Long routeId);
    List<DeliveryOrder> findByStatusIn(List<OrderStatus> statuses);
}
