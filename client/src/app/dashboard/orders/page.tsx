"use client";

import { LocationPicker } from "@/components/map/LocationPicker";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { api } from "@/lib/api";
import {
    formatTime,
    statusColors,
    statusLabels,
} from "@/lib/utils";
import type {
    DeliveryOrder,
    OrderCreateRequest,
    OrderStatus,
} from "@/types";
import { useEffect, useState } from "react";
import { toast } from "sonner";

export default function OrdersPage() {
    const [orders, setOrders] = useState<DeliveryOrder[]>(
        []
    );
    const [isLoading, setIsLoading] = useState(true);
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingOrder, setEditingOrder] =
        useState<DeliveryOrder | null>(null);
    const [filterStatus, setFilterStatus] = useState<
        OrderStatus | "ALL"
    >("ALL");

    useEffect(() => {
        loadOrders();
    }, []);

    const loadOrders = async () => {
        try {
            const data = await api.getOrders();
            setOrders(data);
        } catch (error) {
            console.error("Failed to load orders:", error);
            toast.error("Không thể tải danh sách đơn hàng");
        } finally {
            setIsLoading(false);
        }
    };

    const handleSubmit = async (
        data: OrderCreateRequest
    ) => {
        try {
            if (editingOrder) {
                await api.updateOrder(
                    editingOrder.id,
                    data
                );
                toast.success(
                    "Cập nhật đơn hàng thành công"
                );
            } else {
                await api.createOrder(data);
                toast.success(
                    "Tạo đơn hàng mới thành công"
                );
            }
            loadOrders();
            setIsDialogOpen(false);
            setEditingOrder(null);
        } catch {
            toast.error("Có lỗi xảy ra");
        }
    };

    const handleDelete = async (id: number) => {
        if (!confirm("Bạn có chắc muốn xóa đơn hàng này?"))
            return;
        try {
            await api.deleteOrder(id);
            toast.success("Xóa đơn hàng thành công");
            loadOrders();
        } catch {
            toast.error("Không thể xóa đơn hàng");
        }
    };

    const handleStatusChange = async (
        id: number,
        status: OrderStatus
    ) => {
        try {
            await api.updateOrderStatus(id, status);
            toast.success("Cập nhật trạng thái thành công");
            loadOrders();
        } catch {
            toast.error("Không thể cập nhật trạng thái");
        }
    };

    const filteredOrders =
        filterStatus === "ALL"
            ? orders
            : orders.filter(
                  (o) => o.status === filterStatus
              );

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
                <div className="flex items-center gap-4">
                    <Select
                        value={filterStatus}
                        onValueChange={(v) =>
                            setFilterStatus(
                                v as OrderStatus | "ALL"
                            )
                        }
                    >
                        <SelectTrigger className="w-40 bg-zinc-800 border-zinc-700">
                            <SelectValue placeholder="Lọc trạng thái" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="ALL">
                                Tất cả
                            </SelectItem>
                            <SelectItem value="PENDING">
                                Chờ xử lý
                            </SelectItem>
                            <SelectItem value="ASSIGNED">
                                Đã gán
                            </SelectItem>
                            <SelectItem value="IN_PROGRESS">
                                Đang giao
                            </SelectItem>
                            <SelectItem value="COMPLETED">
                                Hoàn thành
                            </SelectItem>
                            <SelectItem value="CANCELLED">
                                Đã hủy
                            </SelectItem>
                        </SelectContent>
                    </Select>
                </div>

                <Dialog
                    open={isDialogOpen}
                    onOpenChange={setIsDialogOpen}
                >
                    <DialogTrigger asChild>
                        <Button
                            onClick={() =>
                                setEditingOrder(null)
                            }
                        >
                            + Thêm đơn hàng
                        </Button>
                    </DialogTrigger>
                    <DialogContent className="sm:!max-w-5xl !w-[95vw] bg-zinc-900 border-zinc-800">
                        <DialogHeader>
                            <DialogTitle>
                                {editingOrder
                                    ? "Sửa đơn hàng"
                                    : "Thêm đơn hàng mới"}
                            </DialogTitle>
                        </DialogHeader>
                        <OrderForm
                            initialData={editingOrder}
                            onSubmit={handleSubmit}
                            onCancel={() => {
                                setIsDialogOpen(false);
                                setEditingOrder(null);
                            }}
                        />
                    </DialogContent>
                </Dialog>
            </div>

            {/* Orders Table */}
            <Card className="bg-zinc-900 border-zinc-800">
                <CardHeader>
                    <CardTitle>
                        Danh sách đơn hàng (
                        {filteredOrders.length})
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead>
                                <tr className="border-b border-zinc-800">
                                    <th className="text-left py-3 px-4 text-sm text-zinc-400">
                                        ID
                                    </th>
                                    <th className="text-left py-3 px-4 text-sm text-zinc-400">
                                        Khách hàng
                                    </th>
                                    <th className="text-left py-3 px-4 text-sm text-zinc-400">
                                        Địa chỉ
                                    </th>
                                    <th className="text-left py-3 px-4 text-sm text-zinc-400">
                                        Khung giờ
                                    </th>
                                    <th className="text-left py-3 px-4 text-sm text-zinc-400">
                                        Trạng thái
                                    </th>
                                    <th className="text-left py-3 px-4 text-sm text-zinc-400">
                                        Thao tác
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredOrders.map(
                                    (order) => (
                                        <tr
                                            key={order.id}
                                            className="border-b border-zinc-800/50 hover:bg-zinc-800/30"
                                        >
                                            <td className="py-3 px-4 text-sm">
                                                #{order.id}
                                            </td>
                                            <td className="py-3 px-4">
                                                <div>
                                                    <p className="text-sm font-medium">
                                                        {
                                                            order.customerName
                                                        }
                                                    </p>
                                                    <p className="text-xs text-zinc-400">
                                                        {
                                                            order.customerPhone
                                                        }
                                                    </p>
                                                </div>
                                            </td>
                                            <td className="py-3 px-4 text-sm truncate max-w-[200px]">
                                                {
                                                    order.address
                                                }
                                            </td>
                                            <td className="py-3 px-4 text-sm">
                                                {formatTime(
                                                    order.timeWindowStart
                                                )}{" "}
                                                -{" "}
                                                {formatTime(
                                                    order.timeWindowEnd
                                                )}
                                            </td>
                                            <td className="py-3 px-4">
                                                <Select
                                                    value={
                                                        order.status
                                                    }
                                                    onValueChange={(
                                                        v
                                                    ) =>
                                                        handleStatusChange(
                                                            order.id,
                                                            v as OrderStatus
                                                        )
                                                    }
                                                >
                                                    <SelectTrigger className="w-32 h-8 text-xs">
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
                                                    </SelectTrigger>
                                                    <SelectContent>
                                                        <SelectItem value="PENDING">
                                                            Chờ
                                                            xử
                                                            lý
                                                        </SelectItem>
                                                        <SelectItem value="ASSIGNED">
                                                            Đã
                                                            gán
                                                        </SelectItem>
                                                        <SelectItem value="IN_PROGRESS">
                                                            Đang
                                                            giao
                                                        </SelectItem>
                                                        <SelectItem value="COMPLETED">
                                                            Hoàn
                                                            thành
                                                        </SelectItem>
                                                        <SelectItem value="CANCELLED">
                                                            Đã
                                                            hủy
                                                        </SelectItem>
                                                    </SelectContent>
                                                </Select>
                                            </td>
                                            <td className="py-3 px-4">
                                                <div className="flex gap-2">
                                                    <Button
                                                        variant="ghost"
                                                        size="sm"
                                                        onClick={() => {
                                                            setEditingOrder(
                                                                order
                                                            );
                                                            setIsDialogOpen(
                                                                true
                                                            );
                                                        }}
                                                    >
                                                        Sửa
                                                    </Button>
                                                    <Button
                                                        variant="ghost"
                                                        size="sm"
                                                        className="text-red-500 hover:text-red-400"
                                                        onClick={() =>
                                                            handleDelete(
                                                                order.id
                                                            )
                                                        }
                                                    >
                                                        Xóa
                                                    </Button>
                                                </div>
                                            </td>
                                        </tr>
                                    )
                                )}
                            </tbody>
                        </table>
                        {filteredOrders.length === 0 && (
                            <div className="text-center py-8 text-zinc-400">
                                Không có đơn hàng nào
                            </div>
                        )}
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}

function OrderForm({
    initialData,
    onSubmit,
    onCancel,
}: {
    initialData: DeliveryOrder | null;
    onSubmit: (data: OrderCreateRequest) => void;
    onCancel: () => void;
}) {
    // EVRPTW: Single customer location
    const [formData, setFormData] =
        useState<OrderCreateRequest>({
            customerName: initialData?.customerName || "",
            customerPhone: initialData?.customerPhone || "",
            lat: initialData?.lat || 21.0285,
            lng: initialData?.lng || 105.8542,
            address: initialData?.address || "",
            timeWindowStart:
                initialData?.timeWindowStart || 9,
            timeWindowEnd: initialData?.timeWindowEnd || 12,
            demand: initialData?.demand || 1,
            serviceTime: initialData?.serviceTime || 0.1,
            notes: initialData?.notes || "",
        });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSubmit(formData);
    };

    // Safe parse float to handle empty string
    const safeParseFloat = (
        value: string,
        fallback: number = 0
    ) => {
        const parsed = parseFloat(value);
        return isNaN(parsed) ? fallback : parsed;
    };

    return (
        <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Left column - Map */}
                <div className="space-y-2">
                    <Label className="text-base font-medium">
                        Vị trí giao hàng
                    </Label>
                    <LocationPicker
                        value={{
                            lat: formData.lat,
                            lng: formData.lng,
                        }}
                        onChange={(location) =>
                            setFormData({
                                ...formData,
                                lat: location.lat,
                                lng: location.lng,
                            })
                        }
                        className="h-[380px]"
                    />
                </div>

                {/* Right column - Form fields */}
                <div className="space-y-4">
                    {/* Customer info */}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <Label className="text-sm">
                                Tên khách hàng
                            </Label>
                            <Input
                                value={
                                    formData.customerName
                                }
                                onChange={(e) =>
                                    setFormData({
                                        ...formData,
                                        customerName:
                                            e.target.value,
                                    })
                                }
                                required
                                className="bg-zinc-800 border-zinc-700 h-9"
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label className="text-sm">
                                Số điện thoại
                            </Label>
                            <Input
                                value={
                                    formData.customerPhone
                                }
                                onChange={(e) =>
                                    setFormData({
                                        ...formData,
                                        customerPhone:
                                            e.target.value,
                                    })
                                }
                                required
                                className="bg-zinc-800 border-zinc-700 h-9"
                            />
                        </div>
                    </div>

                    {/* Address */}
                    <div className="space-y-1.5">
                        <Label className="text-sm">
                            Địa chỉ
                        </Label>
                        <Input
                            value={formData.address}
                            onChange={(e) =>
                                setFormData({
                                    ...formData,
                                    address: e.target.value,
                                })
                            }
                            placeholder="VD: 123 Đường ABC, Quận XYZ"
                            required
                            className="bg-zinc-800 border-zinc-700 h-9"
                        />
                    </div>

                    {/* Time windows */}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <Label className="text-sm">
                                Giờ bắt đầu
                            </Label>
                            <Input
                                type="number"
                                min="0"
                                max="24"
                                step="0.5"
                                value={
                                    formData.timeWindowStart
                                }
                                onChange={(e) =>
                                    setFormData({
                                        ...formData,
                                        timeWindowStart:
                                            safeParseFloat(
                                                e.target
                                                    .value,
                                                9
                                            ),
                                    })
                                }
                                required
                                className="bg-zinc-800 border-zinc-700 h-9"
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label className="text-sm">
                                Giờ kết thúc
                            </Label>
                            <Input
                                type="number"
                                min="0"
                                max="24"
                                step="0.5"
                                value={
                                    formData.timeWindowEnd
                                }
                                onChange={(e) =>
                                    setFormData({
                                        ...formData,
                                        timeWindowEnd:
                                            safeParseFloat(
                                                e.target
                                                    .value,
                                                12
                                            ),
                                    })
                                }
                                required
                                className="bg-zinc-800 border-zinc-700 h-9"
                            />
                        </div>
                    </div>

                    {/* Demand & Service time */}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <Label className="text-sm">
                                Nhu cầu (Demand)
                            </Label>
                            <Input
                                type="number"
                                min="0"
                                step="0.1"
                                value={formData.demand}
                                onChange={(e) =>
                                    setFormData({
                                        ...formData,
                                        demand: safeParseFloat(
                                            e.target.value,
                                            1
                                        ),
                                    })
                                }
                                className="bg-zinc-800 border-zinc-700 h-9"
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label className="text-sm">
                                Thời gian phục vụ (h)
                            </Label>
                            <Input
                                type="number"
                                min="0"
                                step="0.05"
                                value={formData.serviceTime}
                                onChange={(e) =>
                                    setFormData({
                                        ...formData,
                                        serviceTime:
                                            safeParseFloat(
                                                e.target
                                                    .value,
                                                0.1
                                            ),
                                    })
                                }
                                className="bg-zinc-800 border-zinc-700 h-9"
                            />
                        </div>
                    </div>

                    {/* Notes */}
                    <div className="space-y-1.5">
                        <Label className="text-sm">
                            Ghi chú
                        </Label>
                        <Input
                            value={formData.notes || ""}
                            onChange={(e) =>
                                setFormData({
                                    ...formData,
                                    notes: e.target.value,
                                })
                            }
                            placeholder="Ghi chú thêm (nếu có)"
                            className="bg-zinc-800 border-zinc-700 h-9"
                        />
                    </div>

                    {/* Actions */}
                    <div className="flex justify-end gap-2 pt-2">
                        <Button
                            type="button"
                            variant="ghost"
                            onClick={onCancel}
                        >
                            Hủy
                        </Button>
                        <Button type="submit">
                            {initialData
                                ? "Cập nhật"
                                : "Tạo mới"}
                        </Button>
                    </div>
                </div>
            </div>
        </form>
    );
}
