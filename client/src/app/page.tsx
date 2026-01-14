'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/auth';

export default function Home() {
  const router = useRouter();
  const { isAuthenticated, user, isLoading, checkAuth } = useAuthStore();

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  useEffect(() => {
    if (isLoading) return;

    if (!isAuthenticated) {
      router.push('/login');
    } else if (user?.role === 'DRIVER') {
      router.push('/driver');
    } else {
      router.push('/dashboard');
    }
  }, [isAuthenticated, user, isLoading, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-zinc-900">
      <div className="flex flex-col items-center gap-4">
        <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
        <p className="text-zinc-400">Đang chuyển hướng...</p>
      </div>
    </div>
  );
}
