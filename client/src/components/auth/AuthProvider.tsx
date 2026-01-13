'use client';

import { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuthStore } from '@/lib/auth';

interface AuthProviderProps {
  children: React.ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const { isAuthenticated, isLoading, checkAuth, user } = useAuthStore();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  useEffect(() => {
    if (isLoading) return;

    const isLoginPage = pathname === '/login';
    const isDriverPage = pathname.startsWith('/driver');
    const isDashboardPage = pathname.startsWith('/dashboard') || pathname === '/';

    if (!isAuthenticated && !isLoginPage) {
      router.push('/login');
      return;
    }

    if (isAuthenticated && isLoginPage) {
      if (user?.role === 'DRIVER') {
        router.push('/driver');
      } else {
        router.push('/dashboard');
      }
      return;
    }

    // Role-based access control
    if (isAuthenticated && user) {
      if (user.role === 'DRIVER' && isDashboardPage) {
        router.push('/driver');
        return;
      }
      if (user.role !== 'DRIVER' && isDriverPage && !user.driverId) {
        router.push('/dashboard');
        return;
      }
    }
  }, [isAuthenticated, isLoading, pathname, router, user]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-zinc-900">
        <div className="flex flex-col items-center gap-4">
          <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-zinc-400">Đang tải...</p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
