import type {
    AssignedRoute,
    AuthResponse,
    DeliveryOrder,
    Driver,
    DriverCreateRequest,
    DriverStatus,
    OptimizationRequest,
    OptimizationResponse,
    OrderCreateRequest,
    OrderStatus,
    RouteAssignRequest,
    SwapStation,
    SwapStationCreateRequest,
    User,
} from "@/types";
import axios, { AxiosError, AxiosInstance } from "axios";

const API_BASE_URL =
    process.env.NEXT_PUBLIC_API_URL ||
    "http://localhost:8080";

class ApiClient {
    private client: AxiosInstance;

    constructor() {
        this.client = axios.create({
            baseURL: API_BASE_URL,
            headers: {
                "Content-Type": "application/json",
            },
        });

        // Add auth token to requests
        this.client.interceptors.request.use((config) => {
            const token =
                typeof window !== "undefined"
                    ? localStorage.getItem("token")
                    : null;
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
            return config;
        });

        // Handle auth errors
        this.client.interceptors.response.use(
            (response) => response,
            (error: AxiosError) => {
                if (error.response?.status === 401) {
                    if (typeof window !== "undefined") {
                        localStorage.removeItem("token");
                        window.location.href = "/login";
                    }
                }
                return Promise.reject(error);
            }
        );
    }

    // Auth
    async login(
        username: string,
        password: string
    ): Promise<AuthResponse> {
        const response =
            await this.client.post<AuthResponse>(
                "/api/auth/login",
                { username, password }
            );
        return response.data;
    }

    async getCurrentUser(): Promise<User> {
        const response = await this.client.get<User>(
            "/api/auth/me"
        );
        return response.data;
    }

    // Drivers (Driver = Vehicle in this model)
    async getDrivers(): Promise<Driver[]> {
        const response = await this.client.get<Driver[]>(
            "/api/drivers"
        );
        return response.data;
    }

    async getDriver(id: number): Promise<Driver> {
        const response = await this.client.get<Driver>(
            `/api/drivers/${id}`
        );
        return response.data;
    }

    async getAvailableDrivers(): Promise<Driver[]> {
        const response = await this.client.get<Driver[]>(
            "/api/drivers/available"
        );
        return response.data;
    }

    async createDriver(
        data: DriverCreateRequest
    ): Promise<Driver> {
        const response = await this.client.post<Driver>(
            "/api/drivers",
            data
        );
        return response.data;
    }

    async updateDriver(
        id: number,
        data: DriverCreateRequest
    ): Promise<Driver> {
        const response = await this.client.put<Driver>(
            `/api/drivers/${id}`,
            data
        );
        return response.data;
    }

    async updateDriverStatus(
        id: number,
        status: DriverStatus
    ): Promise<Driver> {
        const response = await this.client.put<Driver>(
            `/api/drivers/${id}/status?status=${status}`
        );
        return response.data;
    }

    async deleteDriver(id: number): Promise<void> {
        await this.client.delete(`/api/drivers/${id}`);
    }

    // Swap Stations
    async getStations(): Promise<SwapStation[]> {
        const response = await this.client.get<
            SwapStation[]
        >("/api/swap-stations");
        return response.data;
    }

    async getActiveStations(): Promise<SwapStation[]> {
        const response = await this.client.get<
            SwapStation[]
        >("/api/swap-stations/active");
        return response.data;
    }

    async getStation(id: number): Promise<SwapStation> {
        const response = await this.client.get<SwapStation>(
            `/api/swap-stations/${id}`
        );
        return response.data;
    }

    async createStation(
        data: SwapStationCreateRequest
    ): Promise<SwapStation> {
        const response =
            await this.client.post<SwapStation>(
                "/api/swap-stations",
                data
            );
        return response.data;
    }

    async updateStation(
        id: number,
        data: SwapStationCreateRequest
    ): Promise<SwapStation> {
        const response = await this.client.put<SwapStation>(
            `/api/swap-stations/${id}`,
            data
        );
        return response.data;
    }

    async toggleStationActive(
        id: number
    ): Promise<SwapStation> {
        const response = await this.client.put<SwapStation>(
            `/api/swap-stations/${id}/toggle-active`
        );
        return response.data;
    }

    async deleteStation(id: number): Promise<void> {
        await this.client.delete(
            `/api/swap-stations/${id}`
        );
    }

    // Orders
    async getOrders(): Promise<DeliveryOrder[]> {
        const response = await this.client.get<
            DeliveryOrder[]
        >("/api/orders");
        return response.data;
    }

    async getOrder(id: number): Promise<DeliveryOrder> {
        const response =
            await this.client.get<DeliveryOrder>(
                `/api/orders/${id}`
            );
        return response.data;
    }

    async getPendingOrders(): Promise<DeliveryOrder[]> {
        const response = await this.client.get<
            DeliveryOrder[]
        >("/api/orders/pending");
        return response.data;
    }

    async getOrdersByStatus(
        status: OrderStatus
    ): Promise<DeliveryOrder[]> {
        const response = await this.client.get<
            DeliveryOrder[]
        >(`/api/orders/status/${status}`);
        return response.data;
    }

    async createOrder(
        data: OrderCreateRequest
    ): Promise<DeliveryOrder> {
        const response =
            await this.client.post<DeliveryOrder>(
                "/api/orders",
                data
            );
        return response.data;
    }

    async updateOrder(
        id: number,
        data: OrderCreateRequest
    ): Promise<DeliveryOrder> {
        const response =
            await this.client.put<DeliveryOrder>(
                `/api/orders/${id}`,
                data
            );
        return response.data;
    }

    async updateOrderStatus(
        id: number,
        status: OrderStatus
    ): Promise<DeliveryOrder> {
        const response =
            await this.client.put<DeliveryOrder>(
                `/api/orders/${id}/status?status=${status}`
            );
        return response.data;
    }

    async deleteOrder(id: number): Promise<void> {
        await this.client.delete(`/api/orders/${id}`);
    }

    // Routes
    async getRoutes(): Promise<AssignedRoute[]> {
        const response = await this.client.get<
            AssignedRoute[]
        >("/api/routes");
        return response.data;
    }

    async getRoute(id: number): Promise<AssignedRoute> {
        const response =
            await this.client.get<AssignedRoute>(
                `/api/routes/${id}`
            );
        return response.data;
    }

    async getRoutesByDriver(
        driverId: number
    ): Promise<AssignedRoute[]> {
        const response = await this.client.get<
            AssignedRoute[]
        >(`/api/routes/driver/${driverId}`);
        return response.data;
    }

    async getMyRoutes(): Promise<AssignedRoute[]> {
        const response = await this.client.get<
            AssignedRoute[]
        >("/api/routes/my-routes");
        return response.data;
    }

    async getMyActiveRoutes(): Promise<AssignedRoute[]> {
        const response = await this.client.get<
            AssignedRoute[]
        >("/api/routes/my-routes/active");
        return response.data;
    }

    async assignRoute(
        data: RouteAssignRequest
    ): Promise<AssignedRoute> {
        const response =
            await this.client.post<AssignedRoute>(
                "/api/routes/assign",
                data
            );
        return response.data;
    }

    async startRoute(id: number): Promise<AssignedRoute> {
        const response =
            await this.client.put<AssignedRoute>(
                `/api/routes/${id}/start`
            );
        return response.data;
    }

    async completeStop(
        routeId: number,
        stopSequence: number
    ): Promise<AssignedRoute> {
        const response =
            await this.client.put<AssignedRoute>(
                `/api/routes/${routeId}/complete-stop?stopSequence=${stopSequence}`
            );
        return response.data;
    }

    async deleteRoute(id: number): Promise<void> {
        await this.client.delete(`/api/routes/${id}`);
    }

    // Optimization
    async optimizeFleet(
        data: OptimizationRequest
    ): Promise<OptimizationResponse> {
        const response =
            await this.client.post<OptimizationResponse>(
                "/api/v1/optimize/fleet",
                data
            );
        return response.data;
    }

    async applyOptimization(
        routes: OptimizationResponse["routes"],
        orderIds: number[]
    ): Promise<AssignedRoute[]> {
        const response = await this.client.post<
            AssignedRoute[]
        >("/api/routes/apply-optimization", {
            routes,
            orderIds,
        });
        return response.data;
    }
}

export const api = new ApiClient();
