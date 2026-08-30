"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Button } from "@/components/ui/button";
import { authStorage } from "@/lib/auth";
import { User } from "@/types";
import { NotificationBell } from "@/components/notifications/NotificationBell";
import { 
  Building2, 
  CalendarDays, 
  PlusCircle, 
  User as UserIcon, 
  LogOut, 
  Menu, 
  X,
  Compass,
  Ticket,
  Sparkles
} from "lucide-react";

export function Navbar() {
  const pathname = usePathname();
  const [user, setUser] = useState<User | null>(null);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    setUser(authStorage.getUser());
  }, [pathname]);

  const handleLogout = () => {
    authStorage.removeToken();
    setUser(null);
    window.location.href = "/";
  };

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/80 backdrop-blur-md">
      <div className="container mx-auto flex h-16 items-center justify-between px-4 sm:px-8">
        {/* Brand Logo */}
        <Link href="/" className="flex items-center gap-2.5 group">
          <div className="h-9 w-9 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-500 flex items-center justify-center text-white shadow-md group-hover:scale-105 transition-transform">
            <Compass className="h-5 w-5" />
          </div>
          <div className="flex flex-col">
            <span className="font-bold text-lg leading-tight bg-clip-text text-transparent bg-gradient-to-r from-emerald-700 to-teal-800 dark:from-emerald-400 dark:to-teal-300">
              CivicPulse
            </span>
            <span className="text-[10px] text-muted-foreground font-medium tracking-wider uppercase">
              Community & Events
            </span>
          </div>
        </Link>

        {/* Desktop Navigation Links */}
        <nav className="hidden md:flex items-center gap-6 text-sm font-medium">
          <Link
            href="/events"
            className={`flex items-center gap-1.5 transition-colors hover:text-primary ${
              pathname.startsWith("/events") ? "text-primary font-semibold" : "text-muted-foreground"
            }`}
          >
            <CalendarDays className="h-4 w-4" />
            Discover Events
          </Link>
          <Link
            href="/organizations"
            className={`flex items-center gap-1.5 transition-colors hover:text-primary ${
              pathname.startsWith("/organizations") ? "text-primary font-semibold" : "text-muted-foreground"
            }`}
          >
            <Building2 className="h-4 w-4" />
            Organizations
          </Link>
          {user && (
            <Link
              href="/dashboard"
              className={`flex items-center gap-1.5 transition-colors hover:text-primary ${
                pathname === "/dashboard" ? "text-primary font-semibold" : "text-muted-foreground"
              }`}
            >
              <Sparkles className="h-4 w-4" />
              Dashboard
            </Link>
          )}
        </nav>

        {/* Action Controls & Auth */}
        <div className="hidden md:flex items-center gap-3">
          {user ? (
            <div className="flex items-center gap-3">
              <NotificationBell />
              <Link href="/my-events">
                <Button variant="ghost" size="sm" className="gap-1.5 text-xs text-muted-foreground hover:text-foreground">
                  <Ticket className="h-3.5 w-3.5" />
                  My Passes
                </Button>
              </Link>
              {['ORGANIZER', 'ADMIN'].includes(user.role) && (
                <Link href="/manage-events/create">
                  <Button size="sm" variant="gradient" className="gap-1.5 shadow-sm">
                    <PlusCircle className="h-4 w-4" />
                    Host Event
                  </Button>
                </Link>
              )}

              <div className="flex items-center gap-2 pl-2 border-l">
                <Link href="/profile" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
                  <div className="h-8 w-8 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 flex items-center justify-center font-semibold text-xs border border-emerald-300/40">
                    {user.fullName ? user.fullName.charAt(0).toUpperCase() : "U"}
                  </div>
                  <div className="flex flex-col text-left">
                    <span className="text-xs font-semibold leading-none">{user.fullName}</span>
                    <span className="text-[10px] text-muted-foreground">{user.role}</span>
                  </div>
                </Link>
                <Button variant="ghost" size="icon" onClick={handleLogout} title="Log out" className="h-8 w-8 text-muted-foreground hover:text-destructive">
                  <LogOut className="h-4 w-4" />
                </Button>
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-2.5">
              <Link href="/login">
                <Button variant="ghost" size="sm">
                  Sign In
                </Button>
              </Link>
              <Link href="/register">
                <Button variant="gradient" size="sm">
                  Get Started
                </Button>
              </Link>
            </div>
          )}
        </div>

        {/* Mobile menu trigger */}
        <div className="md:hidden flex items-center">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle menu"
          >
            {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </Button>
        </div>
      </div>

      {/* Mobile dropdown menu */}
      {mobileMenuOpen && (
        <div className="md:hidden border-b border-border bg-background p-4 space-y-3">
          <Link
            href="/events"
            onClick={() => setMobileMenuOpen(false)}
            className="flex items-center gap-2 py-2 text-sm font-medium"
          >
            <CalendarDays className="h-4 w-4 text-emerald-600" />
            Discover Events
          </Link>
          <Link
            href="/organizations"
            onClick={() => setMobileMenuOpen(false)}
            className="flex items-center gap-2 py-2 text-sm font-medium"
          >
            <Building2 className="h-4 w-4 text-emerald-600" />
            Organizations
          </Link>
          {user ? (
            <>
              <Link
                href="/my-events"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-2 py-2 text-sm font-medium text-foreground hover:text-primary"
              >
                <Ticket className="h-4 w-4 text-emerald-600" />
                My Event Passes
              </Link>
              <Link
                href="/notifications"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-2 py-2 text-sm font-medium text-foreground hover:text-primary"
              >
                <Sparkles className="h-4 w-4 text-emerald-600" />
                Notifications Inbox
              </Link>
              {['ORGANIZER', 'ADMIN'].includes(user.role) && (
                <>
                  <Link
                    href="/manage-events"
                    onClick={() => setMobileMenuOpen(false)}
                    className="flex items-center gap-2 py-2 text-sm font-medium text-foreground hover:text-primary"
                  >
                    <PlusCircle className="h-4 w-4 text-emerald-600" />
                    Host & Manage Events
                  </Link>
                  <Link
                    href="/manage-organizations"
                    onClick={() => setMobileMenuOpen(false)}
                    className="flex items-center gap-2 py-2 text-sm font-medium text-foreground hover:text-primary"
                  >
                    <Building2 className="h-4 w-4 text-emerald-600" />
                    My Organizations
                  </Link>
                </>
              )}
              {['MODERATOR', 'ADMIN'].includes(user.role) && (
                <Link
                  href="/moderation"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center gap-2 py-2 text-sm font-medium text-foreground hover:text-primary"
                >
                  <Sparkles className="h-4 w-4 text-amber-500" />
                  Moderation Console
                </Link>
              )}
              <Link
                href="/profile"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-2 py-2 text-sm font-medium text-foreground hover:text-primary"
              >
                <UserIcon className="h-4 w-4 text-emerald-600" />
                Account Profile
              </Link>
              <div className="pt-3 border-t flex justify-between items-center">
                <div className="flex flex-col">
                  <span className="text-xs font-bold text-foreground">{user.fullName || user.email}</span>
                  <span className="text-[10px] text-muted-foreground">{user.role}</span>
                </div>
                <Button variant="outline" size="sm" onClick={handleLogout} className="gap-1 text-xs">
                  <LogOut className="h-3.5 w-3.5" />
                  Log Out
                </Button>
              </div>
            </>
          ) : (
            <div className="grid grid-cols-2 gap-2 pt-2 border-t">
              <Link href="/login" onClick={() => setMobileMenuOpen(false)}>
                <Button variant="outline" className="w-full" size="sm">
                  Sign In
                </Button>
              </Link>
              <Link href="/register" onClick={() => setMobileMenuOpen(false)}>
                <Button variant="gradient" className="w-full" size="sm">
                  Register
                </Button>
              </Link>
            </div>
          )}
        </div>
      )}
    </header>
  );
}
