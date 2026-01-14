"use client";

import type {
    DeliveryOrder,
    Driver,
    SwapStation,
} from "@/types";
import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

// Dynamic import to avoid SSR issues with Leaflet
const MapContainer = dynamic(
    () =>
        import("react-leaflet").then(
            (mod) => mod.MapContainer
        ),
    { ssr: false }
);

const TileLayer = dynamic(
    () =>
        import("react-leaflet").then(
            (mod) => mod.TileLayer
        ),
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
    () =>
        import("react-leaflet").then((mod) => mod.Polyline),
    { ssr: false }
);

interface FleetMapProps {
    stations?: SwapStation[];
    orders?: DeliveryOrder[];
    drivers?: Driver[];  // Drivers with vehicle info
    routes?: Array<{
        points: [number, number][];
        color: string;
    }>;
    center?: [number, number];
    depotPosition?: [number, number]; // Separate depot position
    showDepot?: boolean;
    driverPosition?: [number, number] | null; // Current driver position (midpoint between stops)
    driverPositions?: Array<{
        position: [number, number];
        driverName: string;
        routeId: number;
    }>; // Multiple drivers for admin/manager view
    zoom?: number;
    className?: string;
}

// Default depot location (Hanoi center)
const DEFAULT_DEPOT: [number, number] = [21.0285, 105.8542];

export function FleetMap({
    stations = [],
    orders = [],
    drivers = [],
    routes = [],
    center = DEFAULT_DEPOT,
    depotPosition = DEFAULT_DEPOT,
    showDepot = true,
    driverPosition = null,
    driverPositions = [],
    zoom = 13,
    className = "",
}: FleetMapProps) {
    const [isClient, setIsClient] = useState(false);
    const [icons, setIcons] = useState<{
        station: L.Icon;
        customer: L.Icon;
        vehicle: L.Icon;
        depot: L.Icon;
        driver: any;
    } | null>(null);

    useEffect(() => {
        setIsClient(true);

        // Import Leaflet and create icons
        import("leaflet").then((L) => {
            // Fix default marker icon issue
            delete (
                L.Icon.Default.prototype as {
                    _getIconUrl?: unknown;
                }
            )._getIconUrl;
            L.Icon.Default.mergeOptions({
                iconRetinaUrl:
                    "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png",
                iconUrl:
                    "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png",
                shadowUrl:
                    "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png",
            });

            setIcons({
                station: L.icon({
                    iconUrl:
                        "data:image/svg+xml;base64," +
                        btoa(`
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#10b981" width="32" height="32">
              <path d="M13 10V3L4 14h7v7l9-11h-7z"/>
            </svg>
          `),
                    iconSize: [32, 32],
                    iconAnchor: [16, 32],
                    popupAnchor: [0, -32],
                }),
                customer: L.icon({
                    iconUrl:
                        "data:image/svg+xml;base64," +
                        btoa(`
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#3b82f6" width="28" height="28">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
            </svg>
          `),
                    iconSize: [28, 28],
                    iconAnchor: [14, 28],
                    popupAnchor: [0, -28],
                }),
                vehicle: L.icon({
                    iconUrl:
                        "data:image/svg+xml;base64," +
                        btoa(`
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#8b5cf6" width="32" height="32">
              <path d="M9 17a2 2 0 11-4 0 2 2 0 014 0zM19 17a2 2 0 11-4 0 2 2 0 014 0z"/>
              <path d="M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0"/>
            </svg>
          `),
                    iconSize: [32, 32],
                    iconAnchor: [16, 32],
                    popupAnchor: [0, -32],
                }),
                depot: L.icon({
                    iconUrl:
                        "data:image/svg+xml;base64," +
                        btoa(`
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#f59e0b" width="36" height="36">
              <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
            </svg>
          `),
                    iconSize: [36, 36],
                    iconAnchor: [18, 36],
                    popupAnchor: [0, -36],
                }),
                driver: L.divIcon({
                    html: '<div style="background:#ffffff;border:3px solid #8b5cf6;border-radius:50%;width:36px;height:36px;display:flex;align-items:center;justify-content:center;font-size:20px;box-shadow:0 2px 8px rgba(0,0,0,0.3);">🛵</div>',
                    iconSize: [40, 40],
                    iconAnchor: [20, 20],
                    popupAnchor: [0, -20],
                    className: "",
                }),
            });
        });
    }, []);

    if (!isClient || !icons) {
        return (
            <div
                className={`bg-zinc-800 rounded-lg flex items-center justify-center ${className}`}
            >
                <div className="text-zinc-400">
                    Đang tải bản đồ...
                </div>
            </div>
        );
    }

    return (
        <div
            className={`rounded-lg overflow-hidden ${className}`}
        >
            <link
                rel="stylesheet"
                href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
                integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
                crossOrigin=""
            />
            <MapContainer
                center={center}
                zoom={zoom}
                style={{ height: "100%", width: "100%" }}
                className="z-0"
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {/* Depot marker */}
                {showDepot && (
                    <Marker
                        position={depotPosition}
                        icon={icons.depot}
                    >
                        <Popup>
                            <div className="text-sm">
                                <strong>Kho hàng</strong>
                                <br />
                                Điểm xuất phát và kết thúc
                            </div>
                        </Popup>
                    </Marker>
                )}

                {/* Station markers */}
                {stations.map((station) => (
                    <Marker
                        key={`station-${station.id}`}
                        position={[
                            station.lat,
                            station.lng,
                        ]}
                        icon={icons.station}
                    >
                        <Popup>
                            <div className="text-sm">
                                <strong>
                                    {station.name}
                                </strong>
                                <br />
                                {station.address}
                                <br />
                                Pin có sẵn:{" "}
                                {station.availableBatteries}
                                /{station.totalSlots}
                            </div>
                        </Popup>
                    </Marker>
                ))}

                {/* Customer/Order markers - EVRPTW: chỉ có 1 điểm khách hàng */}
                {orders
                    .filter(
                        (order) =>
                            order.lat != null &&
                            order.lng != null
                    )
                    .map((order) => (
                        <Marker
                            key={`order-${order.id}`}
                            position={[
                                order.lat,
                                order.lng,
                            ]}
                            icon={icons.customer}
                        >
                            <Popup>
                                <div className="text-sm">
                                    <strong>
                                        Khách #{order.id}
                                    </strong>
                                    <br />
                                    {order.customerName}
                                    <br />
                                    {order.address}
                                    <br />
                                    TW:{" "}
                                    {order.timeWindowStart?.toFixed(
                                        1
                                    ) ?? "N/A"}
                                    h -{" "}
                                    {order.timeWindowEnd?.toFixed(
                                        1
                                    ) ?? "N/A"}
                                    h
                                </div>
                            </Popup>
                        </Marker>
                    ))}

                {/* Driver/Vehicle markers - Drivers with vehicle info, display at depot */}
                {drivers.map((driver) => (
                    <Marker
                        key={`driver-${driver.id}`}
                        position={depotPosition}
                        icon={icons.driver}
                    >
                        <Popup>
                            <div className="text-sm">
                                <strong>
                                    {driver.licensePlate || `Driver #${driver.id}`}
                                </strong>
                                <br />
                                Tài xế: {driver.name}
                                <br />
                                Trạng thái: {driver.status}
                            </div>
                        </Popup>
                    </Marker>
                ))}

                {/* Route polylines */}
                {routes.map((route, index) => (
                    <Polyline
                        key={`route-${index}`}
                        positions={route.points}
                        color={route.color}
                        weight={3}
                        opacity={0.7}
                    />
                ))}

                {/* Single driver position (for driver's own view) */}
                {driverPosition && (
                    <Marker
                        position={driverPosition}
                        icon={icons.driver}
                    >
                        <Popup>
                            <div className="text-sm">
                                <strong>
                                    📍 Vị trí hiện tại
                                </strong>
                                <br />
                                Bạn đang ở đây
                            </div>
                        </Popup>
                    </Marker>
                )}

                {/* Multiple driver positions (for admin/manager view) */}
                {driverPositions.map((dp, index) => (
                    <Marker
                        key={`driver-pos-${index}`}
                        position={dp.position}
                        icon={icons.driver}
                    >
                        <Popup>
                            <div className="text-sm">
                                <strong>
                                    🛵 {dp.driverName}
                                </strong>
                                <br />
                                Lộ trình #{dp.routeId}
                            </div>
                        </Popup>
                    </Marker>
                ))}
            </MapContainer>
        </div>
    );
}
