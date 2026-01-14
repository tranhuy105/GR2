"use client";

import { FleetMap } from "@/components/map/FleetMap";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import { api } from "@/lib/api";
import {
    formatTime,
    statusColors,
    statusLabels
} from "@/lib/utils";
import type {
    DeliveryOrder,
    Driver,
    SwapStation,
} from "@/types";
import { useEffect, useState } from "react";

export default function DashboardPage() {
    const [orders, setOrders] = useState<DeliveryOrder[]>(
        []
    );
    const [drivers, setDrivers] = useState<Driver[]>([]);
    const [stations, setStations] = useState<SwapStation[]>(
        []
    );
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            const [
                ordersData,
                driversData,
                stationsData,
            ] = await Promise.all([
                api.getOrders(),
                api.getDrivers(),
                api.getActiveStations(),
            ]);
            setOrders(ordersData);
            setDrivers(driversData);
            setStations(stationsData);
        } catch (error) {
            console.error("Failed to load data:", error);
        } finally {
            setIsLoading(false);
        }
    };

    const stats = {
        pendingOrders: orders.filter(
            (o) => o.status === "PENDING"
        ).length,
        activeOrders: orders.filter(
            (o) => o.status === "IN_PROGRESS"
        ).length,
        completedToday: orders.filter((o) => {
            if (o.status !== "COMPLETED" || !o.completedAt)
                return false;
            const today = new Date().toDateString();
            return (
                new Date(o.completedAt).toDateString() ===
                today
            );
        }).length,
        availableDrivers: drivers.filter(
            (d) => d.status === "AVAILABLE"
        ).length,
        onRouteDrivers: drivers.filter(
            (d) => d.status === "ON_ROUTE"
        ).length,
        totalDrivers: drivers.length,
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
            {/* Stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    title="Đơn chờ xử lý"
                    value={stats.pendingOrders}
                    icon="📦"
                    color="yellow"
                />
                <StatCard
                    title="Đang giao"
                    value={stats.activeOrders}
                    icon="🚚"
                    color="blue"
                />
                <StatCard
                    title="Hoàn thành hôm nay"
                    value={stats.completedToday}
                    icon="✅"
                    color="green"
                />
                <StatCard
                    title="Tài xế sẵn sàng"
                    value={stats.availableDrivers}
                    icon="👤"
                    color="purple"
                />
            </div>

            {/* Map and Quick Stats */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Map */}
                <Card className="lg:col-span-2 bg-zinc-900 border-zinc-800">
                    <CardHeader className="pb-2">
                        <CardTitle className="text-lg">
                            Bản đồ đội xe
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <FleetMap
                            stations={stations}
                            orders={orders.filter(
                                (o) =>
                                    o.status ===
                                        "PENDING" ||
                                    o.status ===
                                        "IN_PROGRESS"
                            )}
                            drivers={drivers}
                            className="h-[400px]"
                        />
                    </CardContent>
                </Card>

                {/* Quick stats sidebar */}
                <div className="space-y-4">
                    {/* Drivers */}
                    <Card className="bg-zinc-900 border-zinc-800">
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-zinc-400">
                                Tài xế
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-2">
                            {drivers
                                .slice(0, 5)
                                .map((driver) => (
                                    <div
                                        key={driver.id}
                                        className="flex items-center justify-between"
                                    >
                                        <span className="text-sm">
                                            {driver.name}
                                        </span>
                                        <Badge
                                            className={
                                                statusColors[
                                                    driver
                                                        .status
                                                ]
                                            }
                                        >
                                            {
                                                statusLabels[
                                                    driver
                                                        .status
                                                ]
                                            }
                                        </Badge>
                                    </div>
                                ))}
                            {drivers.length > 5 && (
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    className="w-full text-zinc-400"
                                >
                                    Xem tất cả (
                                    {drivers.length})
                                </Button>
                            )}
                        </CardContent>
                    </Card>


                    {/* Stations */}
                    <Card className="bg-zinc-900 border-zinc-800">
                        <CardHeader className="pb-2">
                            <CardTitle className="text-sm text-zinc-400">
                                Trạm đổi pin
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-2">
                            {stations
                                .slice(0, 4)
                                .map((station) => (
                                    <div
                                        key={station.id}
                                        className="flex items-center justify-between"
                                    >
                                        <span className="text-sm truncate max-w-[150px]">
                                            {station.name}
                                        </span>
                                        <span className="text-xs text-zinc-400">
                                            {
                                                station.availableBatteries
                                            }
                                            /
                                            {
                                                station.totalSlots
                                            }{" "}
                                            pin
                                        </span>
                                    </div>
                                ))}
                        </CardContent>
                    </Card>
                </div>
            </div>

            {/* Recent Orders */}
            <Card className="bg-zinc-900 border-zinc-800">
                <CardHeader>
                    <CardTitle className="text-lg">
                        Đơn hàng gần đây
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead>
                                <tr className="border-b border-zinc-800">
                                    <th className="text-left py-2 px-4 text-sm text-zinc-400">
                                        ID
                                    </th>
                                    <th className="text-left py-2 px-4 text-sm text-zinc-400">
                                        Khách hàng
                                    </th>
                                    <th className="text-left py-2 px-4 text-sm text-zinc-400">
                                        Địa chỉ giao
                                    </th>
                                    <th className="text-left py-2 px-4 text-sm text-zinc-400">
                                        Thời gian
                                    </th>
                                    <th className="text-left py-2 px-4 text-sm text-zinc-400">
                                        Trạng thái
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {orders
                                    .slice(0, 5)
                                    .map((order) => (
                                        <tr
                                            key={order.id}
                                            className="border-b border-zinc-800/50"
                                        >
                                            <td className="py-3 px-4 text-sm">
                                                #{order.id}
                                            </td>
                                            <td className="py-3 px-4 text-sm">
                                                {
                                                    order.customerName
                                                }
                                            </td>
                                            <td className="py-3 px-4 text-sm truncate max-w-[200px]">
                                                {
                                                    order.address
                                                }
                                            </td>
                                            <td className="py-3 px-4 text-sm text-zinc-400">
                                                {formatTime(
                                                    order.timeWindowStart
                                                )}{" "}
                                                -{" "}
                                                {formatTime(
                                                    order.timeWindowEnd
                                                )}
                                            </td>
                                            <td className="py-3 px-4">
                                                <Badge
                                                    className={
                                                        statusColors[
                                                            order
                                                                .status
                                                        ]
                                                    }
                                                >
                                                    {
                                                        statusLabels[
                                                            order
                                                                .status
                                                        ]
                                                    }
                                                </Badge>
                                            </td>
                                        </tr>
                                    ))}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}

function StatCard({
    title,
    value,
    icon,
    color,
}: {
    title: string;
    value: number;
    icon: string;
    color: "yellow" | "blue" | "green" | "purple";
}) {
    const colorClasses = {
        yellow: "from-yellow-500/20 to-yellow-600/10 border-yellow-500/30",
        blue: "from-blue-500/20 to-blue-600/10 border-blue-500/30",
        green: "from-green-500/20 to-green-600/10 border-green-500/30",
        purple: "from-purple-500/20 to-purple-600/10 border-purple-500/30",
    };

    return (
        <Card
            className={`bg-gradient-to-br ${colorClasses[color]} border`}
        >
            <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                    <div>
                        <p className="text-sm text-zinc-400">
                            {title}
                        </p>
                        <p className="text-3xl font-bold mt-1">
                            {value}
                        </p>
                    </div>
                    <span className="text-3xl">{icon}</span>
                </div>
            </CardContent>
        </Card>
    );
}
