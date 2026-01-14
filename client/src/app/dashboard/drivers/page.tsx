'use client';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger
} from '@/components/ui/select';
import { api } from '@/lib/api';
import { statusColors, statusLabels } from '@/lib/utils';
import type { Driver, DriverCreateRequest, DriverStatus } from '@/types';
import { useEffect, useState } from 'react';
import { toast } from 'sonner';

export default function DriversPage() {
  const [drivers, setDrivers] = useState<Driver[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingDriver, setEditingDriver] = useState<Driver | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const driversData = await api.getDrivers();
      setDrivers(driversData);
    } catch (error) {
      console.error('Failed to load data:', error);
      toast.error('Không thể tải dữ liệu');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (data: DriverCreateRequest) => {
    try {
      if (editingDriver) {
        await api.updateDriver(editingDriver.id, data);
        toast.success('Cập nhật tài xế thành công');
      } else {
        await api.createDriver(data);
        toast.success('Thêm tài xế mới thành công');
      }
      loadData();
      setIsDialogOpen(false);
      setEditingDriver(null);
    } catch {
      toast.error('Có lỗi xảy ra');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Bạn có chắc muốn xóa tài xế này?')) return;
    try {
      await api.deleteDriver(id);
      toast.success('Xóa tài xế thành công');
      loadData();
    } catch {
      toast.error('Không thể xóa tài xế');
    }
  };

  const handleStatusChange = async (id: number, status: DriverStatus) => {
    try {
      await api.updateDriverStatus(id, status);
      toast.success('Cập nhật trạng thái thành công');
      loadData();
    } catch {
      toast.error('Không thể cập nhật trạng thái');
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
          <h2 className="text-xl font-semibold">Quản lý tài xế</h2>
          <p className="text-sm text-zinc-400">
            {drivers.filter(d => d.status === 'AVAILABLE').length} sẵn sàng / {drivers.length} tổng
          </p>
        </div>
        
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button onClick={() => setEditingDriver(null)}>
              + Thêm tài xế
            </Button>
          </DialogTrigger>
          <DialogContent className="bg-zinc-900 border-zinc-800">
            <DialogHeader>
              <DialogTitle>
                {editingDriver ? 'Sửa thông tin tài xế' : 'Thêm tài xế mới'}
              </DialogTitle>
            </DialogHeader>
            <DriverForm
              initialData={editingDriver}
              onSubmit={handleSubmit}
              onCancel={() => {
                setIsDialogOpen(false);
                setEditingDriver(null);
              }}
            />
          </DialogContent>
        </Dialog>
      </div>

      {/* Drivers Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {drivers.map((driver) => (
          <Card key={driver.id} className="bg-zinc-900 border-zinc-800">
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">{driver.name}</CardTitle>
                <Select
                  value={driver.status}
                  onValueChange={(v) => handleStatusChange(driver.id, v as DriverStatus)}
                >
                  <SelectTrigger className="w-32 h-8">
                    <Badge className={statusColors[driver.status]}>
                      {statusLabels[driver.status]}
                    </Badge>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="AVAILABLE">Sẵn sàng</SelectItem>
                    <SelectItem value="ON_ROUTE">Đang giao</SelectItem>
                    <SelectItem value="OFFLINE">Ngoại tuyến</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex items-center gap-2 text-sm text-zinc-400">
                <span>📞</span>
                <span>{driver.phone}</span>
              </div>
              {driver.licensePlate && (
                <div className="flex items-center gap-2 text-sm text-zinc-400">
                  <span>🛵</span>
                  <span>{driver.licensePlate}</span>
                </div>
              )}
              {driver.batteryCapacity && (
                <div className="flex items-center gap-2 text-sm text-zinc-400">
                  <span>🔋</span>
                  <span>Dung lượng: {driver.batteryCapacity} kWh</span>
                </div>
              )}
              <div className="flex gap-2 pt-2">
                <Button
                  variant="ghost"
                  size="sm"
                  className="flex-1"
                  onClick={() => {
                    setEditingDriver(driver);
                    setIsDialogOpen(true);
                  }}
                >
                  Sửa
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="flex-1 text-red-500 hover:text-red-400"
                  onClick={() => handleDelete(driver.id)}
                >
                  Xóa
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {drivers.length === 0 && (
        <div className="text-center py-8 text-zinc-400">
          Chưa có tài xế nào
        </div>
      )}
    </div>
  );
}

function DriverForm({
  initialData,
  onSubmit,
  onCancel,
}: {
  initialData: Driver | null;
  onSubmit: (data: DriverCreateRequest) => void;
  onCancel: () => void;
}) {
  const [formData, setFormData] = useState<DriverCreateRequest>({
    name: initialData?.name || '',
    phone: initialData?.phone || '',
    licensePlate: initialData?.licensePlate || '',
    batteryCapacity: initialData?.batteryCapacity,
    loadCapacity: initialData?.loadCapacity,
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <Label>Tên tài xế</Label>
        <Input
          value={formData.name}
          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          required
          className="bg-zinc-800 border-zinc-700"
        />
      </div>

      <div className="space-y-2">
        <Label>Số điện thoại</Label>
        <Input
          value={formData.phone}
          onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
          required
          className="bg-zinc-800 border-zinc-700"
        />
      </div>

      <div className="space-y-2">
        <Label>Biển số xe</Label>
        <Input
          value={formData.licensePlate || ''}
          onChange={(e) => setFormData({ ...formData, licensePlate: e.target.value })}
          placeholder="VD: 59A1-001"
          className="bg-zinc-800 border-zinc-700"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label>Dung lượng pin (kWh)</Label>
          <Input
            type="number"
            value={formData.batteryCapacity || ''}
            onChange={(e) => setFormData({ ...formData, batteryCapacity: e.target.value ? parseFloat(e.target.value) : undefined })}
            placeholder="77.75"
            className="bg-zinc-800 border-zinc-700"
          />
        </div>
        <div className="space-y-2">
          <Label>Tải trọng (kg)</Label>
          <Input
            type="number"
            value={formData.loadCapacity || ''}
            onChange={(e) => setFormData({ ...formData, loadCapacity: e.target.value ? parseFloat(e.target.value) : undefined })}
            placeholder="200"
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
