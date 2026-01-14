"use client";

import { useMapEvents } from "react-leaflet";

interface ClickHandlerProps {
    onLocationSelect: (location: { lat: number; lng: number }) => void;
}

export function ClickHandler({ onLocationSelect }: ClickHandlerProps) {
    useMapEvents({
        click: (e) => {
            onLocationSelect({
                lat: e.latlng.lat,
                lng: e.latlng.lng,
            });
        },
    });
    return null;
}
