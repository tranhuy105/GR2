'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { api } from '@/lib/api';
import { statusColors, statusLabels, formatBattery } from '@/lib/utils';
import { toast } from 'sonner';
import type { Vehicle, VehicleCreateRequest } from '@/types';

export default function VehiclesPage() {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingVehicle, setEditingVehicle] = useState<Vehicle | null>(null);

  useEffect(() => {
    loadVehicles();
  }, []);

  const loadVehicles = async () => {
    try {
      const data = await api.getVehicles();
      setVehicles(data);
    } catch (error) {
      console.error('Failed to load vehicles:', error);
      toast.error('Không thể tải danh sách xe');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (data: VehicleCreateRequest) => {
    try {
      if (editingVehicle) {
        await api.updateVehicle(editingVehicle.id, data);
        toast.success('Cập nhật xe thành công');
      } else {
        await api.createVehicle(data);
        toast.success('Thêm xe mới thành công');
      }
      loadVehicles();
      setIsDialogOpen(false);
      setEditingVehicle(null);
    } catch {
      toast.error('Có lỗi xảy ra');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Bạn có chắc muốn xóa xe này?')) return;
    try {
      await api.deleteVehicle(id);
      toast.success('Xóa xe thành công');
      loadVehicles();
    } catch {
      toast.error('Không thể xóa xe');
    }
  };

  const getBatteryColor = (level: number) => {
    if (level >= 60) return 'text-green-500';
    if (level >= 30) return 'text-yellow-500';
    return 'text-red-500';
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
          <h2 className="text-xl font-semibold">Quản lý xe</h2>
          <p className="text-sm text-zinc-400">
            {vehicles.filter(v => v.status === 'AVAILABLE').length} sẵn sàng / {vehicles.length} tổng
          </p>
        </div>
        
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button onClick={() => setEditingVehicle(null)}>
              + Thêm xe
            </Button>
          </DialogTrigger>
          <DialogContent className="bg-zinc-900 border-zinc-800">
            <DialogHeader>
              <DialogTitle>
                {editingVehicle ? 'Sửa thông tin xe' : 'Thêm xe mới'}
              </DialogTitle>
            </DialogHeader>
            <VehicleForm
              initialData={editingVehicle}
              onSubmit={handleSubmit}
              onCancel={() => {
                setIsDialogOpen(false);
                setEditingVehicle(null);
              }}
            />
          </DialogContent>
        </Dialog>
      </div>

      {/* Vehicles Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {vehicles.map((vehicle) => (
          <Card key={vehicle.id} className="bg-zinc-900 border-zinc-800">
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg font-mono">{vehicle.licensePlate}</CardTitle>
                <Badge className={statusColors[vehicle.status]}>
                  {statusLabels[vehicle.status]}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              {/* Battery indicator */}
              <div className="space-y-1">
                <div className="flex justify-between text-sm">
                  <span className="text-zinc-400">Pin</span>
                  <span className={getBatteryColor(vehicle.batteryLevel)}>
                    {formatBattery(vehicle.batteryLevel)}
                  </span>
                </div>
                <div className="h-2 bg-zinc-800 rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all ${
                      vehicle.batteryLevel >= 60
                        ? 'bg-green-500'
                        : vehicle.batteryLevel >= 30
                        ? 'bg-yellow-500'
                        : 'bg-red-500'
                    }`}
                    style={{ width: `${vehicle.batteryLevel}%` }}
                  />
                </div>
              </div>

              {vehicle.currentDriverName && (
                <div className="flex items-center gap-2 text-sm text-zinc-400">
                  <span>👤</span>
                  <span>{vehicle.currentDriverName}</span>
                </div>
              )}

              <div className="flex gap-2 pt-2">
                <Button
                  variant="ghost"
                  size="sm"
                  className="flex-1"
                  onClick={() => {
                    setEditingVehicle(vehicle);
                    setIsDialogOpen(true);
                  }}
                >
                  Sửa
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="flex-1 text-red-500 hover:text-red-400"
                  onClick={() => handleDelete(vehicle.id)}
                >
                  Xóa
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {vehicles.length === 0 && (
        <div className="text-center py-8 text-zinc-400">
          Chưa có xe nào
        </div>
      )}
    </div>
  );
}

function VehicleForm({
  initialData,
  onSubmit,
  onCancel,
}: {
  initialData: Vehicle | null;
  onSubmit: (data: VehicleCreateRequest) => void;
  onCancel: () => void;
}) {
  const [formData, setFormData] = useState<VehicleCreateRequest>({
    licensePlate: initialData?.licensePlate || '',
    batteryCapacity: initialData?.batteryCapacity || 100,
    currentLat: initialData?.currentLat || 21.0285,
    currentLng: initialData?.currentLng || 105.8542,
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <Label>Biển số xe</Label>
        <Input
          value={formData.licensePlate}
          onChange={(e) => setFormData({ ...formData, licensePlate: e.target.value })}
          placeholder="VD: 59A1-12345"
          required
          className="bg-zinc-800 border-zinc-700"
        />
      </div>

      <div className="space-y-2">
        <Label>Dung lượng pin (%)</Label>
        <Input
          type="number"
          min="1"
          max="200"
          value={formData.batteryCapacity}
          onChange={(e) => setFormData({ ...formData, batteryCapacity: parseFloat(e.target.value) })}
          required
          className="bg-zinc-800 border-zinc-700"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label>Vĩ độ hiện tại</Label>
          <Input
            type="number"
            step="0.0001"
            value={formData.currentLat}
            onChange={(e) => setFormData({ ...formData, currentLat: parseFloat(e.target.value) })}
            className="bg-zinc-800 border-zinc-700"
          />
        </div>
        <div className="space-y-2">
          <Label>Kinh độ hiện tại</Label>
          <Input
            type="number"
            step="0.0001"
            value={formData.currentLng}
            onChange={(e) => setFormData({ ...formData, currentLng: parseFloat(e.target.value) })}
            className="bg-zinc-800 border-zinc-700"
          />
        </div>
      </div>

      <div className="flex justify-end gap-2 pt-4">
        <Button type="button" variant="ghost" onClick={onCancel}>
          Hủy
        </Button>
        <Button type="submit">
          {initialData ? 'Cập nhật' : 'Tạo mới'}
        </Button>
      </div>
    </form>
  );
}
