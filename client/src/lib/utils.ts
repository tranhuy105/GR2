import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatTime(hours: number): string {
  const h = Math.floor(hours);
  const m = Math.round((hours - h) * 60);
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
}

export function formatDistance(km: number): string {
  if (km < 1) {
    return `${Math.round(km * 1000)} m`;
  }
  return `${km.toFixed(1)} km`;
}

export function formatBattery(level: number): string {
  return `${Math.round(level)}%`;
}

export const statusColors = {
  // Order status
  PENDING: 'bg-yellow-500',
  ASSIGNED: 'bg-blue-500',
  IN_PROGRESS: 'bg-purple-500',
  COMPLETED: 'bg-green-500',
  CANCELLED: 'bg-red-500',
  
  // Driver status
  AVAILABLE: 'bg-green-500',
  ON_ROUTE: 'bg-blue-500',
  OFFLINE: 'bg-gray-500',
  
  // Vehicle status
  IN_USE: 'bg-blue-500',
  CHARGING: 'bg-yellow-500',
  
  // Route status
  PLANNED: 'bg-yellow-500',
} as const;

export const statusLabels = {
  // Order status
  PENDING: 'Chờ xử lý',
  ASSIGNED: 'Đã gán',
  IN_PROGRESS: 'Đang giao',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  
  // Driver status
  AVAILABLE: 'Sẵn sàng',
  ON_ROUTE: 'Đang giao',
  OFFLINE: 'Ngoại tuyến',
  
  // Vehicle status
  IN_USE: 'Đang sử dụng',
  CHARGING: 'Đang sạc',
  
  // Route status
  PLANNED: 'Đã lên kế hoạch',
} as const;
