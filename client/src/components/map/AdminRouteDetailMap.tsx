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

interface AdminRouteDetailMapProps {
    stops: RouteStop[];
    driverName: string;
    routeId: number;
    depotPosition: [number, number];
    className?: string;
}

const DEFAULT_DEPOT: [number, number] = [21.0285, 105.8542];

export function AdminRouteDetailMap({
    stops,
    driverName,
    routeId,
    depotPosition = DEFAULT_DEPOT,
    className = "",
}: AdminRouteDetailMapProps) {
    const [isClient, setIsClient] = useState(false);
    const [icons, setIcons] = useState<{
        depot: L.Icon;
        completed: L.Icon;
        current: L.Icon;
        upcoming: L.Icon;
        driver: any;
    } | null>(null);

    const sortedStops = [...stops].sort((a, b) => a.sequence - b.sequence);
    const completedStops = sortedStops.filter((s) => s.completed);
    const currentStop = sortedStops.find((s) => !s.completed);
    const upcomingStops = sortedStops.filter(
        (s) => !s.completed && s !== currentStop
    );

    useEffect(() => {
        setIsClient(true);

        import("leaflet").then((L) => {
            delete (L.Icon.Default.prototype as { _getIconUrl?: unknown })._getIconUrl;

            setIcons({
                depot: L.icon({
                    iconUrl: "data:image/svg+xml;base64," + btoa(`
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40" width="40" height="40">
                            <polygon points="20,5 35,18 35,35 5,35 5,18" fill="#f59e0b" stroke="#fff" stroke-width="2"/>
                            <rect x="15" y="22" width="10" height="13" fill="#fff"/>
                        </svg>
                    `),
                    iconSize: [40, 40],
                    iconAnchor: [20, 35],
                    popupAnchor: [0, -35],
                }),
                completed: L.icon({
                    iconUrl: "data:image/svg+xml;base64," + btoa(`
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 28 28" width="28" height="28">
                            <circle cx="14" cy="14" r="12" fill="#10b981" stroke="#fff" stroke-width="2"/>
                            <path d="M9 14 L12 17 L19 10" stroke="#fff" stroke-width="2" fill="none" stroke-linecap="round"/>
                        </svg>
                    `),
                    iconSize: [28, 28],
                    iconAnchor: [14, 14],
                    popupAnchor: [0, -14],
                }),
                current: L.icon({
                    iconUrl: "data:image/svg+xml;base64," + btoa(`
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 36 44" width="36" height="44">
                            <path d="M18 0 C18 0 34 12 34 22 C34 32 28 40 18 40 C8 40 2 32 2 22 C2 12 18 0 18 0 Z" fill="#ef4444" stroke="#fff" stroke-width="2"/>
                            <circle cx="18" cy="20" r="8" fill="#fff"/>
                            <circle cx="18" cy="20" r="4" fill="#ef4444"/>
                        </svg>
                    `),
                    iconSize: [36, 44],
                    iconAnchor: [18, 40],
                    popupAnchor: [0, -40],
                }),
                upcoming: L.icon({
                    iconUrl: "data:image/svg+xml;base64," + btoa(`
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">
                            <circle cx="12" cy="12" r="10" fill="#3b82f6" stroke="#fff" stroke-width="2"/>
                            <circle cx="12" cy="12" r="4" fill="#fff"/>
                        </svg>
                    `),
                    iconSize: [24, 24],
                    iconAnchor: [12, 12],
                    popupAnchor: [0, -12],
                }),
                driver: L.divIcon({
                    html: '<div style="background:#ffffff;border:3px solid #8b5cf6;border-radius:50%;width:42px;height:42px;display:flex;align-items:center;justify-content:center;font-size:22px;box-shadow:0 2px 8px rgba(0,0,0,0.3);">🛵</div>',
                    iconSize: [48, 48],
                    iconAnchor: [24, 24],
                    popupAnchor: [0, -24],
                    className: '',
                }),
            });
        });
    }, []);

    if (!isClient || !icons) {
        return (
            <div className={`bg-zinc-800 rounded-lg flex items-center justify-center ${className}`}>
                <div className="text-zinc-400">Đang tải...</div>
            </div>
        );
    }

    // Driver position - MIDPOINT between last completed and next stop
    const lastCompleted = completedStops[completedStops.length - 1];
    const lastPos: [number, number] = lastCompleted?.lat && lastCompleted?.lng
        ? [lastCompleted.lat, lastCompleted.lng]
        : depotPosition;
    const nextPos: [number, number] = currentStop?.lat && currentStop?.lng
        ? [currentStop.lat, currentStop.lng]
        : depotPosition;
    const driverPos: [number, number] = [
        (lastPos[0] + nextPos[0]) / 2,
        (lastPos[1] + nextPos[1]) / 2
    ];

    // Completed path
    const completedPath: [number, number][] = [depotPosition];
    completedStops.forEach((stop) => {
        if (stop.lat && stop.lng) completedPath.push([stop.lat, stop.lng]);
    });

    // Remaining path
    const remainingPath: [number, number][] = [driverPos];
    if (currentStop?.lat && currentStop?.lng) {
        remainingPath.push([currentStop.lat, currentStop.lng]);
    }
    upcomingStops.forEach((stop) => {
        if (stop.lat && stop.lng) remainingPath.push([stop.lat, stop.lng]);
    });
    remainingPath.push(depotPosition);

    // Center
    const center: [number, number] = driverPos;

    return (
        <div className={`rounded-lg overflow-hidden ${className}`}>
            <link
                rel="stylesheet"
                href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
                crossOrigin=""
            />
            <MapContainer
                center={center}
                zoom={13}
                style={{ height: "100%", width: "100%" }}
            >
                <TileLayer
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {completedPath.length > 1 && (
                    <Polyline positions={completedPath} color="#10b981" weight={4} />
                )}
                {remainingPath.length > 1 && (
                    <Polyline positions={remainingPath} color="#3b82f6" weight={3} dashArray="8, 8" />
                )}

                <Marker position={depotPosition} icon={icons.depot}>
                    <Popup><strong>🏠 Kho hàng</strong></Popup>
                </Marker>

                {completedStops.map((stop) => (
                    <Marker
                        key={`c-${stop.sequence}`}
                        position={[stop.lat!, stop.lng!]}
                        icon={icons.completed}
                    >
                        <Popup>✅ Đã hoàn thành #{stop.sequence}</Popup>
                    </Marker>
                ))}

                {currentStop && currentStop.lat && currentStop.lng && (
                    <Marker position={[currentStop.lat, currentStop.lng]} icon={icons.current}>
                        <Popup>
                            <strong className="text-red-600">🎯 Đích đến tiếp theo</strong>
                            <br />#{currentStop.sequence} - {currentStop.address}
                        </Popup>
                    </Marker>
                )}

                {upcomingStops.map((stop) => (
                    <Marker
                        key={`u-${stop.sequence}`}
                        position={[stop.lat!, stop.lng!]}
                        icon={icons.upcoming}
                    >
                        <Popup>📍 Điểm #{stop.sequence}</Popup>
                    </Marker>
                ))}

                <Marker position={driverPos} icon={icons.driver}>
                    <Popup>
                        <strong>🛵 {driverName}</strong>
                        <br />Lộ trình #{routeId}
                        <br />{completedStops.length}/{sortedStops.length} điểm hoàn thành
                    </Popup>
                </Marker>
            </MapContainer>
        </div>
    );
}
