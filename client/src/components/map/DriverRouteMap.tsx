"use client";

import type { RouteStop } from "@/types";
import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

const MapContainer = dynamic(
    () => import("react-leaflet").then((mod) => mod.MapContainer),
    { ssr: false }
);
const TileLayer = dynamic(
    () => import("react-leaflet").then((mod) => mod.TileLayer),
    { ssr: false }
);
const Marker = dynamic(
    () => import("react-leaflet").then((mod) => mod.Marker),
    { ssr: false }
);
const Popup = dynamic(
    () => import("react-leaflet").then((mod) => mod.Popup),
    { ssr: false }
);
const Polyline = dynamic(
    () => import("react-leaflet").then((mod) => mod.Polyline),
    { ssr: false }
);

interface DriverRouteMapProps {
    stops: RouteStop[];
    currentStopSequence: number; // The next stop to complete
    depotPosition: [number, number];
    className?: string;
}

const DEFAULT_DEPOT: [number, number] = [21.0285, 105.8542];

export function DriverRouteMap({
    stops,
    currentStopSequence,
    depotPosition = DEFAULT_DEPOT,
    className = "",
}: DriverRouteMapProps) {
    const [isClient, setIsClient] = useState(false);
    const [icons, setIcons] = useState<{
        depot: L.Icon;
        completed: L.Icon;
        current: L.Icon;
        upcoming: L.Icon;
        driver: any;
    } | null>(null);

    const sortedStops = [...stops].sort((a, b) => a.sequence - b.sequence);
    const currentStop = sortedStops.find((s) => s.sequence === currentStopSequence);
    const completedStops = sortedStops.filter((s) => s.completed);
    const upcomingStops = sortedStops.filter((s) => !s.completed && s.sequence !== currentStopSequence);

    useEffect(() => {
        setIsClient(true);

        import("leaflet").then((L) => {
            delete (L.Icon.Default.prototype as { _getIconUrl?: unknown })._getIconUrl;
            L.Icon.Default.mergeOptions({
                iconRetinaUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png",
                iconUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png",
                shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png",
            });

            setIcons({
                // Depot - Orange house
                depot: L.icon({
                    iconUrl: "data:image/svg+xml;base64," + btoa(`
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40" width="40" height="40">
                            <polygon points="20,5 35,18 35,35 5,35 5,18" fill="#f59e0b" stroke="#fff" stroke-width="2"/>
                            <rect x="15" y="22" width="10" height="13" fill="#fff"/>
                            <polygon points="20,2 38,17 35,17 20,5 5,17 2,17" fill="#f59e0b" stroke="#fff" stroke-width="1"/>
                        </svg>
                    `),
                    iconSize: [40, 40],
                    iconAnchor: [20, 35],
                    popupAnchor: [0, -35],
                }),
                // Completed stops - Green checkmark
                completed: L.icon({
                    iconUrl: "data:image/svg+xml;base64," + btoa(`
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="32" height="32">
                            <circle cx="16" cy="16" r="14" fill="#10b981" stroke="#fff" stroke-width="2"/>
                            <path d="M10 16 L14 20 L22 12" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                    `),
                    iconSize: [32, 32],
                    iconAnchor: [16, 16],
                    popupAnchor: [0, -16],
                }),
                // Current destination - Red flag/target with pulse effect
                current: L.icon({
                    iconUrl: "data:image/svg+xml;base64," + btoa(`
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 56" width="48" height="56">
                            <ellipse cx="24" cy="52" rx="10" ry="3" fill="rgba(239,68,68,0.3)"/>
                            <path d="M24 0 C24 0 44 16 44 28 C44 40 36 48 24 48 C12 48 4 40 4 28 C4 16 24 0 24 0 Z" fill="#ef4444" stroke="#fff" stroke-width="2"/>
                            <circle cx="24" cy="26" r="10" fill="#fff"/>
                            <circle cx="24" cy="26" r="5" fill="#ef4444"/>
                            <text x="24" y="8" text-anchor="middle" fill="#fff" font-size="10" font-weight="bold">NEXT</text>
                        </svg>
                    `),
                    iconSize: [48, 56],
                    iconAnchor: [24, 52],
                    popupAnchor: [0, -52],
                }),
                // Upcoming stops - Blue circle with number
                upcoming: L.icon({
                    iconUrl: "data:image/svg+xml;base64," + btoa(`
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="32" height="32">
                            <circle cx="16" cy="16" r="14" fill="#3b82f6" stroke="#fff" stroke-width="2"/>
                            <circle cx="16" cy="16" r="6" fill="#fff"/>
                        </svg>
                    `),
                    iconSize: [32, 32],
                    iconAnchor: [16, 16],
                    popupAnchor: [0, -16],
                }),
                // Driver position - Using DivIcon with scooter emoji
                driver: L.divIcon({
                    html: '<div style="background:#ffffff;border:3px solid #8b5cf6;border-radius:50%;width:44px;height:44px;display:flex;align-items:center;justify-content:center;font-size:24px;box-shadow:0 2px 8px rgba(0,0,0,0.3);">🛵</div>',
                    iconSize: [50, 50],
                    iconAnchor: [25, 25],
                    popupAnchor: [0, -25],
                    className: '',
                }),
            });
        });
    }, []);

    if (!isClient || !icons) {
        return (
            <div className={`bg-zinc-800 rounded-lg flex items-center justify-center ${className}`}>
                <div className="text-zinc-400">Đang tải bản đồ...</div>
            </div>
        );
    }

    // Build route line with direction
    const routePoints: [number, number][] = [depotPosition];
    sortedStops.forEach((stop) => {
        if (stop.lat && stop.lng) {
            routePoints.push([stop.lat, stop.lng]);
        }
    });
    routePoints.push(depotPosition);

    // Completed path (green)
    const completedPath: [number, number][] = [depotPosition];
    completedStops.forEach((stop) => {
        if (stop.lat && stop.lng) {
            completedPath.push([stop.lat, stop.lng]);
        }
    });

    // Driver position - MIDPOINT between last completed and next stop
    const lastCompleted = completedStops[completedStops.length - 1];
    const lastPos: [number, number] = lastCompleted?.lat && lastCompleted?.lng
        ? [lastCompleted.lat, lastCompleted.lng]
        : depotPosition;
    const nextPos: [number, number] = currentStop?.lat && currentStop?.lng
        ? [currentStop.lat, currentStop.lng]
        : depotPosition;
    // Calculate midpoint
    const driverPos: [number, number] = [
        (lastPos[0] + nextPos[0]) / 2,
        (lastPos[1] + nextPos[1]) / 2
    ];

    // Remaining path (blue dashed)
    const remainingPath: [number, number][] = [driverPos];
    if (currentStop?.lat && currentStop?.lng) {
        remainingPath.push([currentStop.lat, currentStop.lng]);
    }
    upcomingStops.forEach((stop) => {
        if (stop.lat && stop.lng) {
            remainingPath.push([stop.lat, stop.lng]);
        }
    });
    remainingPath.push(depotPosition);

    // Center on current destination or driver
    const center: [number, number] = currentStop?.lat && currentStop?.lng
        ? [currentStop.lat, currentStop.lng]
        : driverPos;

    return (
        <div className={`rounded-lg overflow-hidden ${className}`}>
            <link
                rel="stylesheet"
                href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
                integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
                crossOrigin=""
            />
            <MapContainer
                center={center}
                zoom={14}
                style={{ height: "100%", width: "100%" }}
                className="z-0"
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {/* Completed path - solid green */}
                {completedPath.length > 1 && (
                    <Polyline
                        positions={completedPath}
                        color="#10b981"
                        weight={5}
                        opacity={0.8}
                    />
                )}

                {/* Remaining path - dashed blue with arrows */}
                {remainingPath.length > 1 && (
                    <Polyline
                        positions={remainingPath}
                        color="#3b82f6"
                        weight={4}
                        opacity={0.8}
                        dashArray="10, 10"
                    />
                )}

                {/* Depot marker */}
                <Marker position={depotPosition} icon={icons.depot}>
                    <Popup>
                        <div className="text-sm font-medium">
                            <strong>🏠 KHO HÀNG</strong>
                            <br />
                            Điểm xuất phát & kết thúc
                        </div>
                    </Popup>
                </Marker>

                {/* Completed stops */}
                {completedStops.map((stop) => (
                    <Marker
                        key={`completed-${stop.sequence}`}
                        position={[stop.lat!, stop.lng!]}
                        icon={icons.completed}
                    >
                        <Popup>
                            <div className="text-sm">
                                <strong>✅ Đã hoàn thành</strong>
                                <br />
                                Điểm #{stop.sequence}
                                {stop.customerName && <><br />👤 {stop.customerName}</>}
                                {stop.address && <><br />📍 {stop.address}</>}
                            </div>
                        </Popup>
                    </Marker>
                ))}

                {/* Current destination - BIG RED TARGET */}
                {currentStop && currentStop.lat && currentStop.lng && (
                    <Marker
                        position={[currentStop.lat, currentStop.lng]}
                        icon={icons.current}
                    >
                        <Popup>
                            <div className="text-sm">
                                <strong className="text-red-600">🎯 ĐÍCH ĐẾN TIẾP THEO</strong>
                                <br />
                                Điểm #{currentStop.sequence}
                                {currentStop.customerName && <><br />👤 {currentStop.customerName}</>}
                                {currentStop.address && <><br />📍 {currentStop.address}</>}
                            </div>
                        </Popup>
                    </Marker>
                )}

                {/* Upcoming stops */}
                {upcomingStops.map((stop) => (
                    <Marker
                        key={`upcoming-${stop.sequence}`}
                        position={[stop.lat!, stop.lng!]}
                        icon={icons.upcoming}
                    >
                        <Popup>
                            <div className="text-sm">
                                <strong>📍 Điểm #{stop.sequence}</strong>
                                {stop.customerName && <><br />👤 {stop.customerName}</>}
                                {stop.address && <><br />📍 {stop.address}</>}
                            </div>
                        </Popup>
                    </Marker>
                ))}

                {/* Driver position - at last completed or depot */}
                <Marker position={driverPos} icon={icons.driver}>
                    <Popup>
                        <div className="text-sm">
                            <strong>🛵 VỊ TRÍ CỦA BẠN</strong>
                            <br />
                            {lastCompleted 
                                ? `Vừa hoàn thành điểm #${lastCompleted.sequence}`
                                : "Đang ở kho hàng"}
                        </div>
                    </Popup>
                </Marker>
            </MapContainer>

            {/* Legend */}
            <div className="absolute bottom-2 left-2 bg-zinc-900/90 rounded-lg p-2 text-xs space-y-1 z-[1000]">
                <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded-full bg-purple-500"></div>
                    <span>Vị trí của bạn</span>
                </div>
                <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded-full bg-red-500"></div>
                    <span>Đích đến tiếp theo</span>
                </div>
                <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded-full bg-green-500"></div>
                    <span>Đã hoàn thành</span>
                </div>
                <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded-full bg-blue-500"></div>
                    <span>Sắp tới</span>
                </div>
                <div className="flex items-center gap-2">
                    <div className="w-3 h-0.5 bg-green-500"></div>
                    <span>Đã đi</span>
                </div>
                <div className="flex items-center gap-2">
                    <div className="w-3 h-0.5 bg-blue-500 border-dashed border-t-2 border-blue-500"></div>
                    <span>Còn lại</span>
                </div>
            </div>
        </div>
    );
}
