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
import { FleetMap } from '@/components/map/FleetMap';
import { api } from '@/lib/api';
import { formatTime } from '@/lib/utils';
import { toast } from 'sonner';
import type { SwapStation, SwapStationCreateRequest } from '@/types';

export default function StationsPage() {
  const [stations, setStations] = useState<SwapStation[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingStation, setEditingStation] = useState<SwapStation | null>(null);

  useEffect(() => {
    loadStations();
  }, []);

  const loadStations = async () => {
    try {
      const data = await api.getStations();
      setStations(data);
    } catch (error) {
      console.error('Failed to load stations:', error);
      toast.error('Không thể tải danh sách trạm');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (data: SwapStationCreateRequest) => {
    try {
      if (editingStation) {
        await api.updateStation(editingStation.id, data);
        toast.success('Cập nhật trạm thành công');
      } else {
        await api.createStation(data);
        toast.success('Thêm trạm mới thành công');
      }
      loadStations();
      setIsDialogOpen(false);
      setEditingStation(null);
    } catch {
      toast.error('Có lỗi xảy ra');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Bạn có chắc muốn xóa trạm này?')) return;
    try {
      await api.deleteStation(id);
      toast.success('Xóa trạm thành công');
      loadStations();
    } catch {
      toast.error('Không thể xóa trạm');
    }
  };

  const handleToggleActive = async (id: number) => {
    try {
      await api.toggleStationActive(id);
      toast.success('Cập nhật trạng thái thành công');
      loadStations();
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
          <h2 className="text-xl font-semibold">Trạm đổi pin</h2>
          <p className="text-sm text-zinc-400">
            {stations.filter(s => s.isActive).length} hoạt động / {stations.length} tổng
          </p>
        </div>
        
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button onClick={() => setEditingStation(null)}>
              + Thêm trạm
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-lg bg-zinc-900 border-zinc-800">
            <DialogHeader>
              <DialogTitle>
                {editingStation ? 'Sửa thông tin trạm' : 'Thêm trạm mới'}
              </DialogTitle>
            </DialogHeader>
            <StationForm
              initialData={editingStation}
              onSubmit={handleSubmit}
              onCancel={() => {
                setIsDialogOpen(false);
                setEditingStation(null);
              }}
            />
          </DialogContent>
        </Dialog>
      </div>

      {/* Map */}
      <Card className="bg-zinc-900 border-zinc-800">
        <CardHeader className="pb-2">
          <CardTitle className="text-lg">Vị trí các trạm</CardTitle>
        </CardHeader>
        <CardContent>
          <FleetMap
            stations={stations.filter(s => s.isActive)}
            className="h-[300px]"
          />
        </CardContent>
      </Card>

      {/* Stations Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {stations.map((station) => (
          <Card key={station.id} className={`bg-zinc-900 border-zinc-800 ${!station.isActive ? 'opacity-60' : ''}`}>
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">{station.name}</CardTitle>
                <Badge className={station.isActive ? 'bg-green-500' : 'bg-gray-500'}>
                  {station.isActive ? 'Hoạt động' : 'Tạm đóng'}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="text-sm text-zinc-400">{station.address}</p>
              
              {/* Battery availability */}
              <div className="space-y-1">
                <div className="flex justify-between text-sm">
                  <span className="text-zinc-400">Pin có sẵn</span>
                  <span className={station.availableBatteries > 3 ? 'text-green-500' : 'text-yellow-500'}>
                    {station.availableBatteries}/{station.totalSlots}
                  </span>
                </div>
                <div className="h-2 bg-zinc-800 rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all ${
                      station.availableBatteries > 5 ? 'bg-green-500' : 'bg-yellow-500'
                    }`}
                    style={{ width: `${(station.availableBatteries / station.totalSlots) * 100}%` }}
                  />
                </div>
              </div>

              <div className="flex items-center gap-2 text-sm text-zinc-400">
                <span>🕐</span>
                <span>{formatTime(station.openTime)} - {formatTime(station.closeTime)}</span>
              </div>

              <div className="flex gap-2 pt-2">
                <Button
                  variant="ghost"
                  size="sm"
                  className="flex-1"
                  onClick={() => handleToggleActive(station.id)}
                >
                  {station.isActive ? 'Tạm đóng' : 'Mở lại'}
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setEditingStation(station);
                    setIsDialogOpen(true);
                  }}
                >
                  Sửa
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-red-500 hover:text-red-400"
                  onClick={() => handleDelete(station.id)}
                >
                  Xóa
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {stations.length === 0 && (
        <div className="text-center py-8 text-zinc-400">
          Chưa có trạm đổi pin nào
        </div>
      )}
    </div>
  );
}

function StationForm({
  initialData,
  onSubmit,
  onCancel,
}: {
  initialData: SwapStation | null;
  onSubmit: (data: SwapStationCreateRequest) => void;
  onCancel: () => void;
}) {
  const [formData, setFormData] = useState<SwapStationCreateRequest>({
    name: initialData?.name || '',
    address: initialData?.address || '',
    lat: initialData?.lat || 21.0285,
    lng: initialData?.lng || 105.8542,
    totalSlots: initialData?.totalSlots || 20,
    availableBatteries: initialData?.availableBatteries || 10,
    openTime: initialData?.openTime || 6,
    closeTime: initialData?.closeTime || 22,
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <Label>Tên trạm</Label>
        <Input
          value={formData.name}
          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          required
          className="bg-zinc-800 border-zinc-700"
        />
      </div>

      <div className="space-y-2">
        <Label>Địa chỉ</Label>
        <Input
          value={formData.address}
          onChange={(e) => setFormData({ ...formData, address: e.target.value })}
          required
          className="bg-zinc-800 border-zinc-700"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label>Vĩ độ</Label>
          <Input
            type="number"
            step="0.0001"
            value={formData.lat}
            onChange={(e) => setFormData({ ...formData, lat: parseFloat(e.target.value) })}
            required
            className="bg-zinc-800 border-zinc-700"
          />
        </div>
        <div className="space-y-2">
          <Label>Kinh độ</Label>
          <Input
            type="number"
            step="0.0001"
            value={formData.lng}
            onChange={(e) => setFormData({ ...formData, lng: parseFloat(e.target.value) })}
            required
            className="bg-zinc-800 border-zinc-700"
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label>Tổng số slot</Label>
          <Input
            type="number"
            min="1"
            value={formData.totalSlots}
            onChange={(e) => setFormData({ ...formData, totalSlots: parseInt(e.target.value) })}
            required
            className="bg-zinc-800 border-zinc-700"
          />
        </div>
        <div className="space-y-2">
          <Label>Pin có sẵn</Label>
          <Input
            type="number"
            min="0"
            value={formData.availableBatteries}
            onChange={(e) => setFormData({ ...formData, availableBatteries: parseInt(e.target.value) })}
            required
            className="bg-zinc-800 border-zinc-700"
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label>Giờ mở cửa</Label>
          <Input
            type="number"
            min="0"
            max="24"
            step="0.5"
            value={formData.openTime}
            onChange={(e) => setFormData({ ...formData, openTime: parseFloat(e.target.value) })}
            required
            className="bg-zinc-800 border-zinc-700"
          />
        </div>
        <div className="space-y-2">
          <Label>Giờ đóng cửa</Label>
          <Input
            type="number"
            min="0"
            max="24"
            step="0.5"
            value={formData.closeTime}
            onChange={(e) => setFormData({ ...formData, closeTime: parseFloat(e.target.value) })}
            required
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
