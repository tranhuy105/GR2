package com.tranhuy105.server.repository;

import com.tranhuy105.server.entity.AssignedRoute;
import com.tranhuy105.server.entity.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignedRouteRepository extends JpaRepository<AssignedRoute, Long> {
    List<AssignedRoute> findByDriverId(Long driverId);
    List<AssignedRoute> findByDriverIdAndStatus(Long driverId, RouteStatus status);
    List<AssignedRoute> findByStatus(RouteStatus status);
}
