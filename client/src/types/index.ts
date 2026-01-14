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

// Driver types (Driver = Vehicle in this model)
export type DriverStatus =
    | "AVAILABLE"
    | "ON_ROUTE"
    | "OFFLINE";

export interface Driver {
    id: number;
    name: string;
    phone: string;
    status: DriverStatus;
    licensePlate?: string;       // Vehicle license plate
    batteryCapacity?: number;    // Vehicle battery capacity
    loadCapacity?: number;       // Vehicle cargo capacity
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
    licensePlate?: string;  // From driver
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
    driverIds?: number[];   // Changed from vehicleIds
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
    driverId?: number;       // Actual driver ID from backend
    driverName?: string;     // Driver name for display
    licensePlate?: string;   // License plate for display
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
    // Soft constraint warning
    insufficientDrivers?: boolean;
    requiredDriverCount?: number;
    availableDriverCount?: number;
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
    licensePlate?: string;
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
    orderIds: number[];
    customStops?: RouteStop[];
}
