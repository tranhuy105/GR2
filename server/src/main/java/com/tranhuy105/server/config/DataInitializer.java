package com.tranhuy105.server.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.tranhuy105.server.entity.DeliveryOrder;
import com.tranhuy105.server.entity.Driver;
import com.tranhuy105.server.entity.DriverStatus;
import com.tranhuy105.server.entity.OrderStatus;
import com.tranhuy105.server.entity.Role;
import com.tranhuy105.server.entity.SwapStation;
import com.tranhuy105.server.entity.User;
import com.tranhuy105.server.entity.Vehicle;
import com.tranhuy105.server.entity.VehicleStatus;
import com.tranhuy105.server.repository.DeliveryOrderRepository;
import com.tranhuy105.server.repository.DriverRepository;
import com.tranhuy105.server.repository.SwapStationRepository;
import com.tranhuy105.server.repository.UserRepository;
import com.tranhuy105.server.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final SwapStationRepository swapStationRepository;
    private final DeliveryOrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Depot position (center point - similar to D0 in test data)
    private static final double DEPOT_LAT = 21.0285;
    private static final double DEPOT_LNG = 105.8542;
    
    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already initialized, skipping...");
            return;
        }
        
        log.info("Initializing demo data based on EVRPTW test case c101C5...");
        
        // Create 5 vehicles with varying battery levels (like real fleet)
        Vehicle vehicle1 = vehicleRepository.save(Vehicle.builder()
                .licensePlate("59A1-001")
                .batteryLevel(100.0)
                .batteryCapacity(77.75) // Q from test data
                .loadCapacity(200.0)    // C from test data
                .status(VehicleStatus.AVAILABLE)
                .currentLat(DEPOT_LAT)
                .currentLng(DEPOT_LNG)
                .build());
        
        Vehicle vehicle2 = vehicleRepository.save(Vehicle.builder()
                .licensePlate("59A1-002")
                .batteryLevel(85.0)
                .batteryCapacity(77.75)
                .loadCapacity(200.0)
                .status(VehicleStatus.AVAILABLE)
                .currentLat(DEPOT_LAT)
                .currentLng(DEPOT_LNG)
                .build());
        
        Vehicle vehicle3 = vehicleRepository.save(Vehicle.builder()
                .licensePlate("59A1-003")
                .batteryLevel(70.0)
                .batteryCapacity(77.75)
                .loadCapacity(200.0)
                .status(VehicleStatus.AVAILABLE)
                .currentLat(DEPOT_LAT)
                .currentLng(DEPOT_LNG)
                .build());
        
        Vehicle vehicle4 = vehicleRepository.save(Vehicle.builder()
                .licensePlate("59A1-004")
                .batteryLevel(90.0)
                .batteryCapacity(77.75)
                .loadCapacity(200.0)
                .status(VehicleStatus.AVAILABLE)
                .currentLat(DEPOT_LAT)
                .currentLng(DEPOT_LNG)
                .build());
        
        Vehicle vehicle5 = vehicleRepository.save(Vehicle.builder()
                .licensePlate("59A1-005")
                .batteryLevel(55.0)  // Low battery - might need swap
                .batteryCapacity(77.75)
                .loadCapacity(200.0)
                .status(VehicleStatus.AVAILABLE)
                .currentLat(DEPOT_LAT)
                .currentLng(DEPOT_LNG)
                .build());
        
        // Create 5 drivers
        Driver driver1 = driverRepository.save(Driver.builder()
                .name("Nguyen Van An")
                .phone("0901001001")
                .status(DriverStatus.AVAILABLE)
                .currentVehicle(vehicle1)
                .build());
        
        Driver driver2 = driverRepository.save(Driver.builder()
                .name("Tran Thi Binh")
                .phone("0901001002")
                .status(DriverStatus.AVAILABLE)
                .currentVehicle(vehicle2)
                .build());
        
        Driver driver3 = driverRepository.save(Driver.builder()
                .name("Le Van Cuong")
                .phone("0901001003")
                .status(DriverStatus.AVAILABLE)
                .currentVehicle(vehicle3)
                .build());
        
        Driver driver4 = driverRepository.save(Driver.builder()
                .name("Pham Thi Dung")
                .phone("0901001004")
                .status(DriverStatus.AVAILABLE)
                .currentVehicle(vehicle4)
                .build());
        
        Driver driver5 = driverRepository.save(Driver.builder()
                .name("Hoang Van Em")
                .phone("0901001005")
                .status(DriverStatus.OFFLINE)
                .build());
        
        // Create users
        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());
        
        userRepository.save(User.builder()
                .username("manager")
                .password(passwordEncoder.encode("manager123"))
                .role(Role.MANAGER)
                .build());
        
        userRepository.save(User.builder()
                .username("driver1")
                .password(passwordEncoder.encode("driver123"))
                .role(Role.DRIVER)
                .driver(driver1)
                .driverName(driver1.getName())
                .build());
        
        userRepository.save(User.builder()
                .username("driver2")
                .password(passwordEncoder.encode("driver123"))
                .role(Role.DRIVER)
                .driver(driver2)
                .driverName(driver2.getName())
                .build());
        
        userRepository.save(User.builder()
                .username("driver3")
                .password(passwordEncoder.encode("driver123"))
                .role(Role.DRIVER)
                .driver(driver3)
                .driverName(driver3.getName())
                .build());
        
        userRepository.save(User.builder()
                .username("driver4")
                .password(passwordEncoder.encode("driver123"))
                .role(Role.DRIVER)
                .driver(driver4)
                .driverName(driver4.getName())
                .build());
        
        // Create swap stations at strategic locations (like S0, S5, S15 in test data)
        // S0 - at depot
        swapStationRepository.save(SwapStation.builder()
                .name("Tram S0 - Kho chinh")
                .address("Depot - Trung tam Ha Noi")
                .lat(DEPOT_LAT)
                .lng(DEPOT_LNG)
                .availableBatteries(20)
                .totalSlots(25)
                .build());
        
        // S5 - Northwest (like 31, 84 scaled to Hanoi)
        swapStationRepository.save(SwapStation.builder()
                .name("Tram S5 - Tay Ho")
                .address("12 Lac Long Quan, Tay Ho, Ha Noi")
                .lat(21.0650)
                .lng(105.8200)
                .availableBatteries(15)
                .totalSlots(20)
                .build());
        
        // S15 - South (like 39, 26 scaled to Hanoi)  
        swapStationRepository.save(SwapStation.builder()
                .name("Tram S15 - Thanh Xuan")
                .address("45 Nguyen Trai, Thanh Xuan, Ha Noi")
                .lat(20.9950)
                .lng(105.8100)
                .availableBatteries(12)
                .totalSlots(18)
                .build());
        
        // Additional stations for coverage
        swapStationRepository.save(SwapStation.builder()
                .name("Tram S10 - Long Bien")
                .address("89 Nguyen Van Cu, Long Bien, Ha Noi")
                .lat(21.0450)
                .lng(105.8800)
                .availableBatteries(10)
                .totalSlots(15)
                .build());
        
        swapStationRepository.save(SwapStation.builder()
                .name("Tram S20 - Cau Giay")
                .address("234 Cau Giay, Cau Giay, Ha Noi")
                .lat(21.0359)
                .lng(105.7850)
                .availableBatteries(18)
                .totalSlots(22)
                .build());
        
        // Create orders based on c101C5 pattern with time windows
        // Similar distribution and time windows as C30, C12, C100, C85, C64
        
        // C30 equivalent - nearby depot, early time window
        orderRepository.save(DeliveryOrder.builder()
                .customerName("Khach hang C30 - Nguyen Van Minh")
                .customerPhone("0912300030")
                .lat(21.0180)  // Southwest from depot
                .lng(105.8400)
                .address("20 Nguyen Du, Hoan Kiem, Ha Noi")
                .timeWindowStart(5.9)   // 355/60 ≈ 5.9h
                .timeWindowEnd(6.8)     // 407/60 ≈ 6.8h
                .demand(10.0)
                .serviceTime(1.5)       // 90/60 = 1.5h
                .status(OrderStatus.PENDING)
                .build());
        
        // C12 equivalent - northwest, early morning
        orderRepository.save(DeliveryOrder.builder()
                .customerName("Khach hang C12 - Tran Thi Lan")
                .customerPhone("0912300012")
                .lat(21.0550)  // Northwest
                .lng(105.8250)
                .address("25 Au Co, Tay Ho, Ha Noi")
                .timeWindowStart(2.9)   // 176/60 ≈ 2.9h
                .timeWindowEnd(3.8)     // 228/60 ≈ 3.8h
                .demand(20.0)
                .serviceTime(1.5)
                .status(OrderStatus.PENDING)
                .build());
        
        // C100 equivalent - northeast, late
        orderRepository.save(DeliveryOrder.builder()
                .customerName("Khach hang C100 - Le Van Hung")
                .customerPhone("0912300100")
                .lat(21.0500)  // Northeast
                .lng(105.8700)
                .address("55 Nguyen Van Cu, Long Bien, Ha Noi")
                .timeWindowStart(12.4)  // 744/60 ≈ 12.4h
                .timeWindowEnd(13.3)    // 798/60 ≈ 13.3h
                .demand(20.0)
                .serviceTime(1.5)
                .status(OrderStatus.PENDING)
                .build());
        
        // C85 equivalent - east, late
        orderRepository.save(DeliveryOrder.builder()
                .customerName("Khach hang C85 - Pham Van Duc")
                .customerPhone("0912300085")
                .lat(21.0300)  // East
                .lng(105.8800)
                .address("68 Truong Dinh, Hai Ba Trung, Ha Noi")
                .timeWindowStart(12.3)  // 737/60 ≈ 12.3h
                .timeWindowEnd(13.5)    // 809/60 ≈ 13.5h
                .demand(30.0)
                .serviceTime(1.5)
                .status(OrderStatus.PENDING)
                .build());
        
        // C64 equivalent - south, mid-morning
        orderRepository.save(DeliveryOrder.builder()
                .customerName("Khach hang C64 - Hoang Thi Mai")
                .customerPhone("0912300064")
                .lat(21.0050)  // South
                .lng(105.8350)
                .address("48 Truong Chinh, Dong Da, Ha Noi")
                .timeWindowStart(4.4)   // 263/60 ≈ 4.4h
                .timeWindowEnd(5.4)     // 325/60 ≈ 5.4h
                .demand(10.0)
                .serviceTime(1.5)
                .status(OrderStatus.PENDING)
                .build());
        
        // Additional orders to make it more realistic
        // C45 - central, morning
        orderRepository.save(DeliveryOrder.builder()
                .customerName("Khach hang C45 - Vu Van Nam")
                .customerPhone("0912300045")
                .lat(21.0320)
                .lng(105.8480)
                .address("45 Hai Ba Trung, Hoan Kiem, Ha Noi")
                .timeWindowStart(8.0)
                .timeWindowEnd(10.0)
                .demand(15.0)
                .serviceTime(1.0)
                .status(OrderStatus.PENDING)
                .build());
        
        // C78 - west, noon
        orderRepository.save(DeliveryOrder.builder()
                .customerName("Khach hang C78 - Nguyen Thi Oanh")
                .customerPhone("0912300078")
                .lat(21.0280)
                .lng(105.7900)
                .address("78 Kim Ma, Ba Dinh, Ha Noi")
                .timeWindowStart(11.0)
                .timeWindowEnd(13.0)
                .demand(25.0)
                .serviceTime(1.5)
                .status(OrderStatus.PENDING)
                .build());
        
        // C23 - north, early
        orderRepository.save(DeliveryOrder.builder()
                .customerName("Khach hang C23 - Tran Van Phuc")
                .customerPhone("0912300023")
                .lat(21.0580)
                .lng(105.8550)
                .address("23 Yen Phu, Tay Ho, Ha Noi")
                .timeWindowStart(7.0)
                .timeWindowEnd(9.0)
                .demand(12.0)
                .serviceTime(1.0)
                .status(OrderStatus.PENDING)
                .build());
        
        // C91 - southeast, afternoon
        orderRepository.save(DeliveryOrder.builder()
                .customerName("Khach hang C91 - Le Thi Quyen")
                .customerPhone("0912300091")
                .lat(21.0100)
                .lng(105.8650)
                .address("91 Minh Khai, Hai Ba Trung, Ha Noi")
                .timeWindowStart(14.0)
                .timeWindowEnd(16.0)
                .demand(18.0)
                .serviceTime(1.0)
                .status(OrderStatus.PENDING)
                .build());
        
        // C56 - central-west, mid-day
        orderRepository.save(DeliveryOrder.builder()
                .customerName("Khach hang C56 - Pham Thi Rong")
                .customerPhone("0912300056")
                .lat(21.0250)
                .lng(105.8150)
                .address("56 Lang Ha, Dong Da, Ha Noi")
                .timeWindowStart(10.0)
                .timeWindowEnd(12.0)
                .demand(20.0)
                .serviceTime(1.0)
                .status(OrderStatus.PENDING)
                .build());
        
        log.info("===========================================");
        log.info("Demo data initialized successfully!");
        log.info("Based on EVRPTW test case c101C5");
        log.info("===========================================");
        log.info("Depot location: {}, {}", DEPOT_LAT, DEPOT_LNG);
        log.info("Vehicles: 5 (battery capacity: 77.75, load: 200)");
        log.info("Drivers: 5 (4 available, 1 offline)");
        log.info("Swap Stations: 5");
        log.info("Orders: 10 (with various time windows)");
        log.info("===========================================");
        log.info("Demo accounts:");
        log.info("  - admin/admin123 (ADMIN)");
        log.info("  - manager/manager123 (MANAGER)");
        log.info("  - driver1/driver123 (DRIVER - Nguyen Van An)");
        log.info("  - driver2/driver123 (DRIVER - Tran Thi Binh)");
        log.info("  - driver3/driver123 (DRIVER - Le Van Cuong)");
        log.info("  - driver4/driver123 (DRIVER - Pham Thi Dung)");
        log.info("===========================================");
    }
}
