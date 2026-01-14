"use client";

import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

// Dynamic imports for Leaflet components
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

// Component that uses useMapEvents - must be inside MapContainer
const ClickHandler = dynamic(
    () => import("./ClickHandler").then((mod) => mod.ClickHandler),
    { ssr: false }
);

interface LocationPickerProps {
    value: { lat: number; lng: number };
    onChange: (location: { lat: number; lng: number }) => void;
    className?: string;
}

export function LocationPicker({
    value,
    onChange,
    className = "",
}: LocationPickerProps) {
    const [isClient, setIsClient] = useState(false);
    const [icon, setIcon] = useState<L.Icon | null>(null);

    useEffect(() => {
        setIsClient(true);

        import("leaflet").then((L) => {
            delete (L.Icon.Default.prototype as { _getIconUrl?: unknown })
                ._getIconUrl;
            L.Icon.Default.mergeOptions({
                iconRetinaUrl:
                    "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png",
                iconUrl:
                    "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png",
                shadowUrl:
                    "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png",
            });

            setIcon(
                L.icon({
                    iconUrl:
                        "data:image/svg+xml;base64," +
                        btoa(`
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#ef4444" width="32" height="32">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
              `),
                    iconSize: [32, 32],
                    iconAnchor: [16, 32],
                    popupAnchor: [0, -32],
                })
            );
        });
    }, []);

    if (!isClient || !icon) {
        return (
            <div
                className={`bg-zinc-800 rounded-lg flex items-center justify-center ${className}`}
            >
                <div className="text-zinc-400 text-sm">Đang tải bản đồ...</div>
            </div>
        );
    }

    return (
        <div className={`rounded-lg overflow-hidden ${className}`}>
            <link
                rel="stylesheet"
                href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
                integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
                crossOrigin=""
            />
            <div className="text-xs text-zinc-400 mb-2 flex items-center gap-2">
                <span>📍</span>
                <span>Click trên bản đồ để chọn vị trí giao hàng</span>
            </div>
            <MapContainer
                center={[value.lat, value.lng]}
                zoom={14}
                style={{ height: "100%", width: "100%", minHeight: "200px" }}
                className="z-0 rounded-lg"
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <ClickHandler onLocationSelect={onChange} />
                <Marker position={[value.lat, value.lng]} icon={icon} />
            </MapContainer>
            <div className="text-xs text-zinc-500 mt-2 text-center">
                Vị trí đã chọn: {value.lat.toFixed(6)}, {value.lng.toFixed(6)}
            </div>
        </div>
    );
}
