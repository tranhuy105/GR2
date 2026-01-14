"use client";

import { AdminRouteDetailMap } from "@/components/map/AdminRouteDetailMap";
import { FleetMap } from "@/components/map/FleetMap";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { api } from "@/lib/api";
import {
    formatDistance,
    statusColors,
    statusLabels,
} from "@/lib/utils";
import type {
    AssignedRoute,
    DeliveryOrder,
    Driver,
    OptimizationResponse,
    RouteAssignRequest,
    SwapStation,
} from "@/types";
import { useEffect, useState } from "react";
import { toast } from "sonner";

export default function RoutesPage() {
    const [routes, setRoutes] = useState<AssignedRoute[]>(
        []
    );
    const [allOrders, setAllOrders] = useState<
        DeliveryOrder[]
    >([]);
    const [drivers, setDrivers] = useState<Driver[]>([]);
    const [stations, setStations] = useState<SwapStation[]>(
        []
    );
    const [isLoading, setIsLoading] = useState(true);
    const [isAssignDialogOpen, setIsAssignDialogOpen] =
        useState(false);
    const [isOptimizing, setIsOptimizing] = useState(false);
    const [isApplying, setIsApplying] = useState(false);
    const [optimizationResult, setOptimizationResult] =
        useState<OptimizationResponse | null>(null);
    const [optimizedOrderIds, setOptimizedOrderIds] =
        useState<number[]>([]);
    const [selectedRoute, setSelectedRoute] =
        useState<AssignedRoute | null>(null);

    // Pending orders for optimization
    const pendingOrders = allOrders.filter(
        (o) => o.status === "PENDING"
    );
    // Orders to show on map (pending + assigned + in progress)
    const mapOrders = allOrders.filter(
        (o) =>
            o.status === "PENDING" ||
            o.status === "ASSIGNED" ||
            o.status === "IN_PROGRESS"
    );

    // Default depot location
    const DEPOT_POSITION: [number, number] = [
        21.0285, 105.8542,
    ];

    // Create route lines for active routes
    const routeColors = [
        "#3b82f6",
        "#10b981",
        "#f59e0b",
        "#ef4444",
        "#8b5cf6",
    ];
    const mapRouteLines = routes
        .filter(
            (r) =>
                r.status === "IN_PROGRESS" ||
                r.status === "PLANNED"
        )
        .map((route, idx) => {
            const points: [number, number][] = [
                DEPOT_POSITION,
            ];
            route.stops
                ?.sort((a, b) => a.sequence - b.sequence)
                .forEach((stop) => {
                    if (stop.lat && stop.lng) {
                        points.push([stop.lat, stop.lng]);
                    }
                });
            points.push(DEPOT_POSITION);
            return {
                points,
                color: routeColors[
                    idx % routeColors.length
                ],
            };
        })
        .filter((r) => r.points.length > 2);

    // Calculate driver positions for IN_PROGRESS routes (admin/manager tracking)
    const driverPositions = routes
        .filter((r) => r.status === "IN_PROGRESS")
        .map((route) => {
            const sortedStops =
                route.stops?.sort(
                    (a, b) => a.sequence - b.sequence
                ) || [];
            const lastCompleted = sortedStops
                .filter((s) => s.completed)
                .pop();
            const nextStop = sortedStops.find(
                (s) => !s.completed
            );

            let position: [number, number] = DEPOT_POSITION;

            if (!lastCompleted && nextStop) {
                // Not started, between depot and first stop
                position = [
                    (DEPOT_POSITION[0] +
                        (nextStop.lat ??
                            DEPOT_POSITION[0])) /
                        2,
                    (DEPOT_POSITION[1] +
                        (nextStop.lng ??
                            DEPOT_POSITION[1])) /
                        2,
                ];
            } else if (lastCompleted && nextStop) {
                // Between two stops
                position = [
                    ((lastCompleted.lat ??
                        DEPOT_POSITION[0]) +
                        (nextStop.lat ??
                            DEPOT_POSITION[0])) /
                        2,
                    ((lastCompleted.lng ??
                        DEPOT_POSITION[1]) +
                        (nextStop.lng ??
                            DEPOT_POSITION[1])) /
                        2,
                ];
            } else if (lastCompleted && !nextStop) {
                // All done, heading back to depot
                position = [
                    ((lastCompleted.lat ??
                        DEPOT_POSITION[0]) +
                        DEPOT_POSITION[0]) /
                        2,
                    ((lastCompleted.lng ??
                        DEPOT_POSITION[1]) +
                        DEPOT_POSITION[1]) /
                        2,
                ];
            }

            return {
                position,
                driverName: route.driverName || "Tài xế",
                routeId: route.id,
            };
        });

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            const [
                routesData,
                ordersData,
                driversData,
                stationsData,
            ] = await Promise.all([
                api.getRoutes(),
                api.getOrders(), // Load ALL orders for map
                api.getAvailableDrivers(),
                api.getActiveStations(),
            ]);
            setRoutes(routesData);
            setAllOrders(ordersData);
            setDrivers(driversData);
            setStations(stationsData);
        } catch (error) {
            console.error("Failed to load data:", error);
            toast.error("Không thể tải dữ liệu");
        } finally {
            setIsLoading(false);
        }
    };

    const handleOptimize = async () => {
        if (pendingOrders.length === 0) {
            toast.error("Không có đơn hàng nào cần tối ưu");
            return;
        }

        const orderIds = pendingOrders.map((o) => o.id);
        setIsOptimizing(true);
        try {
            const result = await api.optimizeFleet({
                orderIds,
                driverIds: drivers.map((d) => d.id),
                stationIds: stations.map((s) => s.id),
                chargingMode: "BATTERY_SWAP",
                batterySwapTime: 5 / 60, // 5 minutes converted to hours
                parallel: true,
            });
            setOptimizationResult(result);
            setOptimizedOrderIds(orderIds);
            toast.success(
                `Tối ưu thành công! ${
                    result.summary.totalVehicles
                } xe, ${formatDistance(
                    result.summary.totalDistance
                )}`
            );
        } catch (error: any) {
            console.error("Optimization error:", error);
            const errorMessage =
                error.response?.data?.messages?.[0] ||
                error.message ||
                "Không thể tối ưu lộ trình";
            toast.error(`Lỗi: ${errorMessage}`);
        } finally {
            setIsOptimizing(false);
        }
    };

    const handleApplyOptimization = async () => {
        if (
            !optimizationResult ||
            optimizedOrderIds.length === 0
        ) {
            toast.error(
                "Không có kết quả tối ưu để áp dụng"
            );
            return;
        }

        setIsApplying(true);
        try {
            await api.applyOptimization(
                optimizationResult.routes,
                optimizedOrderIds
            );
            toast.success(
                `Đã tạo ${optimizationResult.routes.length} lộ trình từ kết quả tối ưu!`
            );
            setOptimizationResult(null);
            setOptimizedOrderIds([]);
            loadData();
        } catch (error: any) {
            console.error(
                "Apply optimization error:",
                error
            );
            const errorMessage =
                error.response?.data?.messages?.[0] ||
                error.message ||
                "Không thể áp dụng kết quả tối ưu";
            toast.error(`Lỗi: ${errorMessage}`);
        } finally {
            setIsApplying(false);
        }
    };

    const handleAssignRoute = async (
        data: RouteAssignRequest
    ) => {
        try {
            await api.assignRoute(data);
            toast.success("Gán lộ trình thành công");
            loadData();
            setIsAssignDialogOpen(false);
        } catch {
            toast.error("Không thể gán lộ trình");
        }
    };

    const handleStartRoute = async (routeId: number) => {
        try {
            await api.startRoute(routeId);
            toast.success("Bắt đầu lộ trình thành công");
            loadData();
        } catch {
            toast.error("Không thể bắt đầu lộ trình");
        }
    };

    const handleDeleteRoute = async (routeId: number) => {
        if (!confirm("Bạn có chắc muốn xóa lộ trình này?"))
            return;
        try {
            await api.deleteRoute(routeId);
            toast.success("Xóa lộ trình thành công");
            loadData();
        } catch {
            toast.error("Không thể xóa lộ trình");
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h2 className="text-xl font-semibold">
                        Quản lý lộ trình
                    </h2>
                    <p className="text-sm text-zinc-400">
                        {pendingOrders.length} đơn chờ xử lý
                        |{" "}
                        {
                            routes.filter(
                                (r) =>
                                    r.status ===
                                    "IN_PROGRESS"
                            ).length
                        }{" "}
                        lộ trình đang chạy
                    </p>
                </div>

                <div className="flex gap-2">
                    <Button
                        onClick={handleOptimize}
                        disabled={
                            isOptimizing ||
                            pendingOrders.length === 0
                        }
                        className="bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700"
                    >
                        {isOptimizing ? (
                            <>
                                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2" />
                                Đang tối ưu...
                            </>
                        ) : (
                            <>⚡ Tối ưu ALNS</>
                        )}
                    </Button>

                    <Dialog
                        open={isAssignDialogOpen}
                        onOpenChange={setIsAssignDialogOpen}
                    >
                        <DialogTrigger asChild>
                            <Button variant="outline">
                                + Gán thủ công
                            </Button>
                        </DialogTrigger>
                        <DialogContent className="max-w-lg bg-zinc-900 border-zinc-800">
                            <DialogHeader>
                                <DialogTitle>
                                    Gán lộ trình thủ công
                                </DialogTitle>
                            </DialogHeader>
                            <AssignRouteForm
                                orders={pendingOrders}
                                drivers={drivers}
                                onSubmit={handleAssignRoute}
                                onCancel={() =>
                                    setIsAssignDialogOpen(
                                        false
                                    )
                                }
                            />
                        </DialogContent>
                    </Dialog>
                </div>
            </div>

            {/* Optimization Result */}
            {optimizationResult && (
                <Card className="bg-gradient-to-r from-green-900/30 to-emerald-900/30 border-green-700">
                    <CardHeader className="pb-2">
                        <div className="flex items-center justify-between">
                            <CardTitle className="text-lg text-green-400">
                                Kết quả tối ưu
                            </CardTitle>
                            <Button
                                variant="ghost"
                                size="sm"
                                onClick={() =>
                                    setOptimizationResult(
                                        null
                                    )
                                }
                                className="text-zinc-400"
                            >
                                ✕
                            </Button>
                        </div>
                    </CardHeader>
                    <CardContent>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
                            <div>
                                <p className="text-sm text-zinc-400">
                                    Số xe
                                </p>
                                <p className="text-2xl font-bold text-green-400">
                                    {
                                        optimizationResult
                                            .summary
                                            .totalVehicles
                                    }
                                </p>
                            </div>
                            <div>
                                <p className="text-sm text-zinc-400">
                                    Tổng quãng đường
                                </p>
                                <p className="text-2xl font-bold text-green-400">
                                    {formatDistance(
                                        optimizationResult
                                            .summary
                                            .totalDistance
                                    )}
                                </p>
                            </div>
                            {/* <div>
                                <p className="text-sm text-zinc-400">
                                    Chi phí
                                </p>
                                <p className="text-2xl font-bold text-green-400">
                                    {optimizationResult.summary.totalCost.toFixed(
                                        0
                                    )}
                                </p>
                            </div> */}
                            <div>
                                <p className="text-sm text-zinc-400">
                                    Thời gian tính
                                </p>
                                <p className="text-2xl font-bold text-green-400">
                                    {(
                                        optimizationResult.computeTimeMs /
                                        1000
                                    ).toFixed(1)}
                                    s
                                </p>
                            </div>
                        </div>

                        {optimizationResult.summary.insufficientDrivers && (
                            <div className="mb-4 p-3 bg-yellow-900/30 border border-yellow-700/50 rounded-lg flex items-center gap-3">
                                <span className="text-xl">⚠️</span>
                                <div>
                                    <p className="text-yellow-400 font-medium">Thiếu tài xế</p>
                                    <p className="text-sm text-zinc-400">
                                        Cần {optimizationResult.summary.requiredDriverCount} tài xế nhưng chỉ có {optimizationResult.summary.availableDriverCount} sẵn sàng.
                                    </p>
                                </div>
                            </div>
                        )}

                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <Badge
                                    className={
                                        optimizationResult
                                            .summary
                                            .feasible
                                            ? "bg-green-500"
                                            : "bg-red-500"
                                    }
                                >
                                    {optimizationResult
                                        .summary.feasible
                                        ? "Khả thi"
                                        : "Không khả thi"}
                                </Badge>
                                <Badge className="bg-blue-500">
                                    {
                                        optimizationResult.chargingMode
                                    }
                                </Badge>
                            </div>
                            <Button
                                onClick={
                                    handleApplyOptimization
                                }
                                disabled={
                                    isApplying ||
                                    !optimizationResult
                                        .summary.feasible
                                }
                                className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700"
                            >
                                {isApplying ? (
                                    <>
                                        <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2" />
                                        Đang áp dụng...
                                    </>
                                ) : (
                                    <>✓ Áp dụng kết quả</>
                                )}
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* Map */}
            <Card className="bg-zinc-900 border-zinc-800">
                <CardHeader className="pb-2">
                    <div className="flex items-center justify-between">
                        <CardTitle className="text-lg">
                            Bản đồ lộ trình
                        </CardTitle>
                        {driverPositions.length > 0 && (
                            <Badge className="bg-red-600">
                                🛵 {driverPositions.length}{" "}
                                tài xế đang giao
                            </Badge>
                        )}
                    </div>
                </CardHeader>
                <CardContent>
                    <FleetMap
                        stations={stations}
                        orders={mapOrders}
                        drivers={drivers}
                        routes={mapRouteLines}
                        depotPosition={DEPOT_POSITION}
                        showDepot={true}
                        driverPositions={driverPositions}
                        className="h-[400px]"
                    />
                </CardContent>
            </Card>

            {/* Routes List */}
            <Card className="bg-zinc-900 border-zinc-800">
                <CardHeader>
                    <CardTitle className="text-lg">
                        Danh sách lộ trình ({routes.length})
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="space-y-4">
                        {routes.map((route) => (
                            <div
                                key={route.id}
                                className={`p-4 rounded-lg border transition-all cursor-pointer ${
                                    route.status ===
                                    "IN_PROGRESS"
                                        ? "bg-purple-900/30 border-purple-600 hover:border-purple-400"
                                        : "bg-zinc-800/50 border-zinc-700 hover:border-zinc-500"
                                }`}
                                onClick={() =>
                                    setSelectedRoute(route)
                                }
                            >
                                <div className="flex items-center justify-between mb-3">
                                    <div className="flex items-center gap-3">
                                        <Badge
                                            className={
                                                statusColors[
                                                    route
                                                        .status
                                                ]
                                            }
                                        >
                                            {
                                                statusLabels[
                                                    route
                                                        .status
                                                ]
                                            }
                                        </Badge>
                                        <span className="font-medium">
                                            Lộ trình #
                                            {route.id}
                                        </span>
                                        {route.status ===
                                            "IN_PROGRESS" && (
                                            <Badge className="bg-purple-600 animate-pulse">
                                                🛵 Đang giao
                                            </Badge>
                                        )}
                                    </div>
                                    <div
                                        className="flex gap-2"
                                        onClick={(e) =>
                                            e.stopPropagation()
                                        }
                                    >
                                        {route.status ===
                                            "IN_PROGRESS" && (
                                            <Button
                                                size="sm"
                                                variant="outline"
                                                className="border-purple-500 text-purple-400"
                                                onClick={() =>
                                                    setSelectedRoute(
                                                        route
                                                    )
                                                }
                                            >
                                                📍 Xem vị
                                                trí
                                            </Button>
                                        )}
                                        {route.status ===
                                            "PLANNED" && (
                                            <Button
                                                size="sm"
                                                onClick={() =>
                                                    handleStartRoute(
                                                        route.id
                                                    )
                                                }
                                            >
                                                Bắt đầu
                                            </Button>
                                        )}
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            className="text-red-500"
                                            onClick={() =>
                                                handleDeleteRoute(
                                                    route.id
                                                )
                                            }
                                        >
                                            Xóa
                                        </Button>
                                    </div>
                                </div>

                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                                    <div>
                                        <span className="text-zinc-400">
                                            Tài xế:
                                        </span>
                                        <p className="font-medium">
                                            {
                                                route.driverName
                                            }
                                        </p>
                                    </div>
                                    <div>
                                        <span className="text-zinc-400">
                                            Xe:
                                        </span>
                                        <p className="font-medium">
                                            {
                                                route.licensePlate
                                            }
                                        </p>
                                    </div>
                                    <div>
                                        <span className="text-zinc-400">
                                            Tiến độ:
                                        </span>
                                        <p className="font-medium">
                                            {
                                                route.completedStops
                                            }
                                            /
                                            {
                                                route.totalStops
                                            }{" "}
                                            điểm
                                        </p>
                                    </div>
                                    <div>
                                        <span className="text-zinc-400">
                                            Quãng đường:
                                        </span>
                                        <p className="font-medium">
                                            {route.totalDistance
                                                ? formatDistance(
                                                      route.totalDistance
                                                  )
                                                : "-"}
                                        </p>
                                    </div>
                                </div>

                                {route.stops &&
                                    route.stops.length >
                                        0 && (
                                        <div className="mt-3 flex flex-wrap gap-2">
                                            {route.stops
                                                .slice(0, 6)
                                                .map(
                                                    (
                                                        stop,
                                                        idx
                                                    ) => (
                                                        <Badge
                                                            key={
                                                                idx
                                                            }
                                                            variant="outline"
                                                            className={
                                                                stop.completed
                                                                    ? "border-green-500 text-green-500"
                                                                    : ""
                                                            }
                                                        >
                                                            {stop.type ===
                                                            "CUSTOMER"
                                                                ? "📍"
                                                                : stop.type ===
                                                                  "SWAP"
                                                                ? "🔋"
                                                                : "🏠"}
                                                            {
                                                                stop.sequence
                                                            }
                                                        </Badge>
                                                    )
                                                )}
                                            {route.stops
                                                .length >
                                                6 && (
                                                <Badge variant="outline">
                                                    +
                                                    {route
                                                        .stops
                                                        .length -
                                                        6}{" "}
                                                    điểm
                                                </Badge>
                                            )}
                                        </div>
                                    )}
                            </div>
                        ))}

                        {routes.length === 0 && (
                            <div className="text-center py-8 text-zinc-400">
                                Chưa có lộ trình nào. Sử
                                dụng nút &quot;Tối ưu
                                ALNS&quot; hoặc &quot;Gán
                                thủ công&quot; để tạo lộ
                                trình.
                            </div>
                        )}
                    </div>
                </CardContent>
            </Card>

            {/* Route Detail Modal */}
            <Dialog
                open={!!selectedRoute}
                onOpenChange={(open) =>
                    !open && setSelectedRoute(null)
                }
            >
                <DialogContent className="sm:!max-w-4xl !w-[95vw] bg-zinc-900 border-zinc-800 max-h-[90vh] overflow-y-auto">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-3">
                            <span>
                                Lộ trình #
                                {selectedRoute?.id}
                            </span>
                            {selectedRoute?.status ===
                                "IN_PROGRESS" && (
                                <Badge className="bg-purple-600">
                                    🛵 Đang giao
                                </Badge>
                            )}
                        </DialogTitle>
                    </DialogHeader>

                    {selectedRoute && (
                        <div className="space-y-4">
                            {/* Driver & Vehicle Info */}
                            <div className="grid grid-cols-2 gap-4 p-4 bg-zinc-800 rounded-lg">
                                <div>
                                    <p className="text-sm text-zinc-400">
                                        Tài xế
                                    </p>
                                    <p className="text-lg font-medium">
                                        {
                                            selectedRoute.driverName
                                        }
                                    </p>
                                </div>
                                <div>
                                    <p className="text-sm text-zinc-400">
                                        Xe
                                    </p>
                                    <p className="text-lg font-medium">
                                        {
                                            selectedRoute.licensePlate
                                        }
                                    </p>
                                </div>
                                <div>
                                    <p className="text-sm text-zinc-400">
                                        Tiến độ
                                    </p>
                                    <p className="text-lg font-medium">
                                        {
                                            selectedRoute.completedStops
                                        }
                                        /
                                        {
                                            selectedRoute.totalStops
                                        }{" "}
                                        điểm
                                    </p>
                                </div>
                                <div>
                                    <p className="text-sm text-zinc-400">
                                        Quãng đường
                                    </p>
                                    <p className="text-lg font-medium">
                                        {selectedRoute.totalDistance
                                            ? formatDistance(
                                                  selectedRoute.totalDistance
                                              )
                                            : "-"}
                                    </p>
                                </div>
                            </div>

                            {/* Map */}
                            {selectedRoute.stops &&
                                selectedRoute.stops.length >
                                    0 && (
                                    <div>
                                        <p className="text-sm text-zinc-400 mb-2">
                                            Bản đồ lộ trình
                                        </p>
                                        <AdminRouteDetailMap
                                            stops={
                                                selectedRoute.stops
                                            }
                                            driverName={
                                                selectedRoute.driverName ||
                                                "Tài xế"
                                            }
                                            routeId={
                                                selectedRoute.id
                                            }
                                            depotPosition={
                                                DEPOT_POSITION
                                            }
                                            className="h-[350px]"
                                        />
                                    </div>
                                )}

                            {/* Stops List */}
                            <div>
                                <p className="text-sm text-zinc-400 mb-2">
                                    Chi tiết các điểm dừng
                                </p>
                                <div className="space-y-2 max-h-[200px] overflow-y-auto">
                                    {selectedRoute.stops
                                        ?.sort(
                                            (a, b) =>
                                                a.sequence -
                                                b.sequence
                                        )
                                        .map(
                                            (stop, idx) => (
                                                <div
                                                    key={
                                                        idx
                                                    }
                                                    className={`flex items-center gap-3 p-2 rounded ${
                                                        stop.completed
                                                            ? "bg-green-900/30"
                                                            : "bg-zinc-800"
                                                    }`}
                                                >
                                                    <div
                                                        className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                                                            stop.completed
                                                                ? "bg-green-600"
                                                                : "bg-zinc-600"
                                                        }`}
                                                    >
                                                        {stop.completed
                                                            ? "✓"
                                                            : stop.sequence}
                                                    </div>
                                                    <div className="flex-1">
                                                        <span className="text-sm">
                                                            {stop.type ===
                                                            "CUSTOMER"
                                                                ? "📍"
                                                                : "🔋"}{" "}
                                                            {stop.customerName ||
                                                                stop.address}
                                                        </span>
                                                    </div>
                                                    {stop.completed && (
                                                        <Badge className="bg-green-600 text-xs">
                                                            Hoàn
                                                            thành
                                                        </Badge>
                                                    )}
                                                </div>
                                            )
                                        )}
                                </div>
                            </div>
                        </div>
                    )}
                </DialogContent>
            </Dialog>
        </div>
    );
}

function AssignRouteForm({
    orders,
    drivers,
    onSubmit,
    onCancel,
}: {
    orders: DeliveryOrder[];
    drivers: Driver[];
    onSubmit: (data: RouteAssignRequest) => void;
    onCancel: () => void;
}) {
    const [selectedOrders, setSelectedOrders] = useState<
        number[]
    >([]);
    const [driverId, setDriverId] = useState<number | null>(
        null
    );

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (
            !driverId ||
            selectedOrders.length === 0
        ) {
            toast.error("Vui lòng chọn đầy đủ thông tin");
            return;
        }
        onSubmit({
            driverId,
            orderIds: selectedOrders,
        });
    };

    const toggleOrder = (orderId: number) => {
        setSelectedOrders((prev) =>
            prev.includes(orderId)
                ? prev.filter((id) => id !== orderId)
                : [...prev, orderId]
        );
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
                <Label>Tài xế (kèm xe)</Label>
                <Select
                    onValueChange={(v) =>
                        setDriverId(parseInt(v))
                    }
                >
                    <SelectTrigger className="bg-zinc-800 border-zinc-700">
                        <SelectValue placeholder="Chọn tài xế" />
                    </SelectTrigger>
                    <SelectContent>
                        {drivers.map((driver) => (
                            <SelectItem
                                key={driver.id}
                                value={driver.id.toString()}
                            >
                                {driver.name} {driver.licensePlate ? `(${driver.licensePlate})` : ''}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>

            <div className="space-y-2">
                <Label>
                    Đơn hàng ({selectedOrders.length} đã
                    chọn)
                </Label>
                <div className="max-h-48 overflow-y-auto space-y-2 p-2 bg-zinc-800 rounded-lg">
                    {orders.map((order) => (
                        <label
                            key={order.id}
                            className={`flex items-center gap-2 p-2 rounded cursor-pointer transition-colors ${
                                selectedOrders.includes(
                                    order.id
                                )
                                    ? "bg-blue-600/30"
                                    : "hover:bg-zinc-700"
                            }`}
                        >
                            <Input
                                type="checkbox"
                                checked={selectedOrders.includes(
                                    order.id
                                )}
                                onChange={() =>
                                    toggleOrder(order.id)
                                }
                                className="w-4 h-4"
                            />
                            <div className="flex-1">
                                <p className="text-sm font-medium">
                                    #{order.id} -{" "}
                                    {order.customerName}
                                </p>
                                <p className="text-xs text-zinc-400 truncate">
                                    {order.address}
                                </p>
                            </div>
                        </label>
                    ))}
                    {orders.length === 0 && (
                        <p className="text-sm text-zinc-400 text-center py-4">
                            Không có đơn hàng nào chờ xử lý
                        </p>
                    )}
                </div>
            </div>

            <div className="flex justify-end gap-2 pt-4">
                <Button
                    type="button"
                    variant="ghost"
                    onClick={onCancel}
                >
                    Hủy
                </Button>
                <Button
                    type="submit"
                    disabled={
                        !driverId ||
                        selectedOrders.length === 0
                    }
                >
                    Gán lộ trình
                </Button>
            </div>
        </form>
    );
}

