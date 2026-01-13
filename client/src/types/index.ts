// User & Auth types
export type Role = "ADMIN" | "MANAGER" | "DRIVER";

export interface User {
    id: number;
    username: string;
    role: Role;
    driverId?: number;
    driverName?: string;
}

export interface AuthResponse {
    token: string;
    username: string;
    role: Role;
    userId: number;
    driverId?: number;
}

// Driver types
export type DriverStatus =
    | "AVAILABLE"
    | "ON_ROUTE"
    | "OFFLINE";

export interface Driver {
    id: number;
    name: string;
    phone: string;
    status: DriverStatus;
    currentVehicleId?: number;
    currentVehiclePlate?: string;
}

// Vehicle types
export type VehicleStatus =
    | "AVAILABLE"
    | "IN_USE"
    | "CHARGING";

export interface Vehicle {
    id: number;
    licensePlate: string;
    batteryLevel: number;
    batteryCapacity: number;
    status: VehicleStatus;
    currentDriverId?: number;
    currentDriverName?: string;
    currentLat?: number;
    currentLng?: number;
}

// SwapStation types
export interface SwapStation {
    id: number;
    name: string;
    address: string;
    lat: number;
    lng: number;
    availableBatteries: number;
    totalSlots: number;
    openTime: number;
    closeTime: number;
    isActive: boolean;
}

// Order types
export type OrderStatus =
    | "PENDING"
    | "ASSIGNED"
    | "IN_PROGRESS"
    | "COMPLETED"
    | "CANCELLED";

export interface DeliveryOrder {
    id: number;
    customerName: string;
    customerPhone: string;
    // EVRPTW: Chỉ có 1 điểm khách hàng (giao từ kho đến)
    lat: number;
    lng: number;
    address: string;
    timeWindowStart: number;
    timeWindowEnd: number;
    demand: number;
    serviceTime: number;
    status: OrderStatus;
    assignedRouteId?: number;
    createdAt: string;
    completedAt?: string;
    notes?: string;
}

// Route types
export type RouteStatus =
    | "PLANNED"
    | "IN_PROGRESS"
    | "COMPLETED"
    | "CANCELLED";

export interface RouteStop {
    sequence: number;
    type: "CUSTOMER" | "SWAP" | "DEPOT";
    orderId?: number;
    stationId?: number;
    lat: number;
    lng: number;
    address: string;
    customerName?: string;
    completed: boolean;
}

export interface AssignedRoute {
    id: number;
    driverId: number;
    driverName: string;
    vehicleId: number;
    vehiclePlate: string;
    status: RouteStatus;
    stops: RouteStop[];
    totalDistance?: number;
    estimatedTime?: number;
    totalStops: number;
    completedStops: number;
    createdAt: string;
    startedAt?: string;
    completedAt?: string;
    orders?: DeliveryOrder[];
}

// Optimization types
export type ChargingMode = "FULL_RECHARGE" | "BATTERY_SWAP";

export interface OptimizationRequest {
    orderIds: number[];
    vehicleIds?: number[];
    stationIds?: number[];
    chargingMode?: ChargingMode;
    batterySwapTime?: number;
    batteryCapacity?: number;
    consumptionRate?: number;
    velocity?: number;
    cargoCapacity?: number;
    iterations?: number;
    timeLimit?: number;
    parallel?: boolean;
    depotLat?: number;
    depotLng?: number;
}

export interface OptimizedStop {
    nodeId: number;
    stringId: string;
    type: string;
    x: number;
    y: number;
}

export interface OptimizedRoute {
    vehicleId: number;
    stops: OptimizedStop[];
    distance: number;
    feasible: boolean;
}

export interface OptimizationSummary {
    totalVehicles: number;
    totalDistance: number;
    totalCost: number;
    feasible: boolean;
    totalCustomers: number;
    totalStations: number;
}

export interface OptimizationResponse {
    routes: OptimizedRoute[];
    summary: OptimizationSummary;
    computeTimeMs: number;
    chargingMode: string;
}

// Request types
export interface OrderCreateRequest {
    customerName: string;
    customerPhone: string;
    // EVRPTW: Chỉ có 1 điểm khách hàng
    lat: number;
    lng: number;
    address: string;
    timeWindowStart: number;
    timeWindowEnd: number;
    demand?: number;
    serviceTime?: number;
    notes?: string;
}

export interface DriverCreateRequest {
    name: string;
    phone: string;
    vehicleId?: number;
}

export interface VehicleCreateRequest {
    licensePlate: string;
    batteryCapacity?: number;
    currentLat?: number;
    currentLng?: number;
}

export interface SwapStationCreateRequest {
    name: string;
    address: string;
    lat: number;
    lng: number;
    totalSlots?: number;
    availableBatteries?: number;
    openTime?: number;
    closeTime?: number;
}

export interface RouteAssignRequest {
    driverId: number;
    vehicleId: number;
    orderIds: number[];
    customStops?: RouteStop[];
}
