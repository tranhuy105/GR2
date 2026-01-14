"use client";

import { AuthProvider } from "@/components/auth/AuthProvider";
import { DriverRouteMap } from "@/components/map/DriverRouteMap";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { api } from "@/lib/api";
import { useAuthStore } from "@/lib/auth";
import { statusColors, statusLabels } from "@/lib/utils";
import type { AssignedRoute } from "@/types";
import { useRouter } from "next/navigation";
import React, { useEffect, useState } from "react";
import { toast } from "sonner";

export default function DriverPage() {
    return (
        <AuthProvider>
            <DriverContent />
        </AuthProvider>
    );
}

function DriverContent() {
    const { user, logout } = useAuthStore();
    const router = useRouter();
    const [routes, setRoutes] = useState<AssignedRoute[]>(
        []
    );
    const [isLoading, setIsLoading] = useState(true);
    const [activeRoute, setActiveRoute] =
        useState<AssignedRoute | null>(null);

    useEffect(() => {
        loadData();
        // Refresh every 30 seconds
        const interval = setInterval(loadData, 30000);
        return () => clearInterval(interval);
    }, []);

    const loadData = async () => {
        try {
            const routesData = await api.getMyRoutes();
            setRoutes(routesData);

            // Find active route
            const active = routesData.find(
                (r) => r.status === "IN_PROGRESS"
            );
            setActiveRoute(active || null);
        } catch (error) {
            console.error("Failed to load data:", error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleStartRoute = async (routeId: number) => {
        try {
            await api.startRoute(routeId);
            toast.success("Bắt đầu lộ trình thành công!");
            loadData();
        } catch {
            toast.error("Không thể bắt đầu lộ trình");
        }
    };

    const handleCompleteStop = async (
        routeId: number,
        stopSequence: number
    ) => {
        try {
            await api.completeStop(routeId, stopSequence);
            toast.success("Đã hoàn thành điểm dừng!");
            loadData();
        } catch {
            toast.error("Không thể cập nhật trạng thái");
        }
    };

    if (isLoading) {
        return (
            <div className="min-h-screen bg-zinc-950 flex items-center justify-center">
                <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-zinc-950 text-white">
            {/* Header */}
            <header className="bg-zinc-900 border-b border-zinc-800 p-4">
                <div className="flex items-center justify-between max-w-4xl mx-auto">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-full flex items-center justify-center">
                            <span className="text-lg">
                                {user?.username?.[0]?.toUpperCase()}
                            </span>
                        </div>
                        <div>
                            <p className="font-medium">
                                {user?.driverName ||
                                    user?.username}
                            </p>
                            <p className="text-xs text-zinc-400">
                                Tài xế
                            </p>
                        </div>
                    </div>
                    <Button
                        variant="ghost"
                        onClick={logout}
                    >
                        Đăng xuất
                    </Button>
                </div>
            </header>

            <main className="max-w-4xl mx-auto p-4 space-y-6">
                {/* Active Route */}
                {activeRoute ? (
                    <ActiveRouteCard
                        route={activeRoute}
                        onCompleteStop={handleCompleteStop}
                    />
                ) : (
                    <Card className="bg-zinc-900 border-zinc-800">
                        <CardContent className="py-12 text-center">
                            <div className="text-5xl mb-4">
                                🛵
                            </div>
                            <h3 className="text-lg font-medium mb-2">
                                Không có lộ trình đang chạy
                            </h3>
                            <p className="text-zinc-400">
                                Bạn chưa có lộ trình nào
                                đang thực hiện. Kiểm tra
                                danh sách lộ trình bên dưới.
                            </p>
                        </CardContent>
                    </Card>
                )}

                {/* Planned Routes */}
                <Card className="bg-zinc-900 border-zinc-800">
                    <CardHeader>
                        <CardTitle className="text-lg">
                            Lộ trình của bạn (
                            {routes.length})
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        {routes.length === 0 ? (
                            <p className="text-center text-zinc-400 py-4">
                                Chưa có lộ trình nào được
                                gán cho bạn
                            </p>
                        ) : (
                            routes.map((route) => (
                                <div
                                    key={route.id}
                                    className="p-4 bg-zinc-800/50 rounded-lg border border-zinc-700"
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
                                        </div>
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
                                    </div>

                                    <div className="grid grid-cols-2 gap-4 text-sm">
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
                                    </div>
                                </div>
                            ))
                        )}
                    </CardContent>
                </Card>

                {/* Quick Stats */}
                <div className="grid grid-cols-3 gap-4">
                    <Card className="bg-zinc-900 border-zinc-800">
                        <CardContent className="py-4 text-center">
                            <p className="text-2xl font-bold text-blue-500">
                                {
                                    routes.filter(
                                        (r) =>
                                            r.status ===
                                            "PLANNED"
                                    ).length
                                }
                            </p>
                            <p className="text-xs text-zinc-400">
                                Chờ thực hiện
                            </p>
                        </CardContent>
                    </Card>
                    <Card className="bg-zinc-900 border-zinc-800">
                        <CardContent className="py-4 text-center">
                            <p className="text-2xl font-bold text-purple-500">
                                {
                                    routes.filter(
                                        (r) =>
                                            r.status ===
                                            "IN_PROGRESS"
                                    ).length
                                }
                            </p>
                            <p className="text-xs text-zinc-400">
                                Đang chạy
                            </p>
                        </CardContent>
                    </Card>
                    <Card className="bg-zinc-900 border-zinc-800">
                        <CardContent className="py-4 text-center">
                            <p className="text-2xl font-bold text-green-500">
                                {
                                    routes.filter(
                                        (r) =>
                                            r.status ===
                                            "COMPLETED"
                                    ).length
                                }
                            </p>
                            <p className="text-xs text-zinc-400">
                                Hoàn thành
                            </p>
                        </CardContent>
                    </Card>
                </div>
            </main>
        </div>
    );
}

// Default depot location
const DEPOT_POSITION: [number, number] = [
    21.0285, 105.8542,
];

function ActiveRouteCard({
    route,
    onCompleteStop,
}: {
    route: AssignedRoute;
    onCompleteStop: (
        routeId: number,
        stopSequence: number
    ) => void;
}) {
    const nextStopRef = React.useRef<HTMLDivElement>(null);
    const sortedStops =
        route.stops?.sort(
            (a, b) => a.sequence - b.sequence
        ) || [];
    const nextStop = sortedStops.find((s) => !s.completed);
    const progress =
        route.totalStops > 0
            ? (route.completedStops / route.totalStops) *
              100
            : 0;

    // Auto scroll to next stop when it changes
    React.useEffect(() => {
        if (nextStopRef.current) {
            nextStopRef.current.scrollIntoView({
                behavior: "smooth",
                block: "center",
            });
        }
    }, [nextStop?.sequence]);

    const getStopIcon = (type: string) => {
        switch (type) {
            case "CUSTOMER":
                return "📍";
            case "SWAP":
                return "🔋";
            case "SWAP_STATION":
                return "🔋";
            default:
                return "📌";
        }
    };

    const getStopLabel = (type: string) => {
        switch (type) {
            case "CUSTOMER":
                return "Giao hàng";
            case "SWAP":
                return "Đổi pin";
            case "SWAP_STATION":
                return "Đổi pin";
            default:
                return type;
        }
    };

    return (
        <div className="space-y-4">
            {/* Progress Header */}
            <Card className="bg-gradient-to-r from-blue-900/40 to-purple-900/40 border-blue-600">
                <CardContent className="py-4">
                    <div className="flex items-center justify-between mb-3">
                        <div>
                            <p className="text-sm text-zinc-400">
                                Lộ trình đang chạy
                            </p>
                            <p className="text-xl font-bold">
                                #{route.id}
                            </p>
                        </div>
                        <div className="text-right">
                            <p className="text-sm text-zinc-400">
                                Tiến độ
                            </p>
                            <p className="text-xl font-bold text-cyan-400">
                                {route.completedStops}/
                                {route.totalStops}
                            </p>
                        </div>
                    </div>
                    <div className="h-3 bg-zinc-800 rounded-full overflow-hidden">
                        <div
                            className="h-full bg-gradient-to-r from-green-500 to-cyan-500 transition-all duration-500"
                            style={{
                                width: `${progress}%`,
                            }}
                        />
                    </div>
                </CardContent>
            </Card>

            {/* Next Stop - BIG AND PROMINENT */}
            {nextStop && (
                <Card className="bg-gradient-to-r from-red-900/40 to-orange-900/40 border-red-500 border-2">
                    <CardHeader className="pb-2">
                        <div className="flex items-center justify-between">
                            <CardTitle className="text-lg text-red-400">
                                🎯 ĐÍCH ĐẾN TIẾP THEO
                            </CardTitle>
                            <Badge className="bg-red-600 animate-pulse text-base px-3 py-1">
                                #{nextStop.sequence}
                            </Badge>
                        </div>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="p-4 bg-zinc-800/80 rounded-lg border border-zinc-700">
                            <div className="flex items-start gap-3">
                                <span className="text-3xl">
                                    {getStopIcon(
                                        nextStop.type
                                    )}
                                </span>
                                <div className="flex-1">
                                    <p className="text-lg font-semibold text-white">
                                        {getStopLabel(
                                            nextStop.type
                                        )}
                                    </p>
                                    <p className="text-zinc-300 mt-1">
                                        {nextStop.address}
                                    </p>
                                    {nextStop.customerName && (
                                        <p className="text-cyan-400 mt-2 font-medium">
                                            👤{" "}
                                            {
                                                nextStop.customerName
                                            }
                                        </p>
                                    )}
                                </div>
                            </div>
                        </div>

                        <Button
                            className="w-full h-14 text-lg font-bold bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-500 hover:to-emerald-500 shadow-lg"
                            onClick={() =>
                                onCompleteStop(
                                    route.id,
                                    nextStop.sequence
                                )
                            }
                        >
                            ✅ ĐÃ HOÀN THÀNH - TIẾP TỤC
                        </Button>
                    </CardContent>
                </Card>
            )}

            {/* Map - Using new DriverRouteMap */}
            <Card className="bg-zinc-900 border-zinc-800">
                <CardHeader className="pb-2">
                    <CardTitle className="text-sm text-zinc-400">
                        Bản đồ lộ trình
                    </CardTitle>
                </CardHeader>
                <CardContent className="relative">
                    <DriverRouteMap
                        stops={sortedStops}
                        currentStopSequence={
                            nextStop?.sequence || 0
                        }
                        depotPosition={DEPOT_POSITION}
                        className="h-[300px]"
                    />
                </CardContent>
            </Card>

            {/* All Stops Timeline */}
            <Card className="bg-zinc-900 border-zinc-800">
                <CardHeader className="pb-2">
                    <CardTitle className="text-sm text-zinc-400">
                        Lộ trình chi tiết
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="space-y-1 max-h-[250px] overflow-y-auto">
                        {/* Start point */}
                        <div className="flex items-center gap-3 p-2 bg-orange-900/20 rounded-lg border border-orange-700">
                            <div className="w-8 h-8 rounded-full bg-orange-600 flex items-center justify-center text-sm">
                                🏠
                            </div>
                            <span className="text-sm font-medium text-orange-300">
                                Xuất phát từ kho
                            </span>
                        </div>

                        {/* Stops */}
                        {sortedStops.map((stop, idx) => {
                            const isNext =
                                stop === nextStop;
                            const isCompleted =
                                stop.completed;
                            return (
                                <div
                                    key={idx}
                                    ref={
                                        isNext
                                            ? nextStopRef
                                            : null
                                    }
                                    className={`flex items-center gap-3 p-2 rounded-lg transition-all ${
                                        isCompleted
                                            ? "bg-green-900/30 border border-green-700"
                                            : isNext
                                            ? "bg-red-900/40 border-2 border-red-500 shadow-lg shadow-red-500/20"
                                            : "bg-zinc-800/50 border border-zinc-700"
                                    }`}
                                >
                                    <div
                                        className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${
                                            isCompleted
                                                ? "bg-green-600"
                                                : isNext
                                                ? "bg-red-600 animate-pulse"
                                                : "bg-zinc-600"
                                        }`}
                                    >
                                        {isCompleted
                                            ? "✓"
                                            : stop.sequence}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2">
                                            <span className="text-lg">
                                                {getStopIcon(
                                                    stop.type
                                                )}
                                            </span>
                                            <span
                                                className={`text-sm font-medium ${
                                                    isNext
                                                        ? "text-red-300"
                                                        : ""
                                                }`}
                                            >
                                                {getStopLabel(
                                                    stop.type
                                                )}
                                                {isNext &&
                                                    " ← ĐÍCH ĐẾN"}
                                            </span>
                                        </div>
                                        <p className="text-xs text-zinc-400 truncate">
                                            {stop.address}
                                        </p>
                                    </div>
                                    {!isCompleted &&
                                        !isNext && (
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="text-xs"
                                                onClick={() =>
                                                    onCompleteStop(
                                                        route.id,
                                                        stop.sequence
                                                    )
                                                }
                                            >
                                                Bỏ qua
                                            </Button>
                                        )}
                                </div>
                            );
                        })}

                        {/* End point */}
                        <div className="flex items-center gap-3 p-2 bg-orange-900/20 rounded-lg border border-orange-700">
                            <div className="w-8 h-8 rounded-full bg-orange-600 flex items-center justify-center text-sm">
                                🏠
                            </div>
                            <span className="text-sm font-medium text-orange-300">
                                Quay về kho
                            </span>
                        </div>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
