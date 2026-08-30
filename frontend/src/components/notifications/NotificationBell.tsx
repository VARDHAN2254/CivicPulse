"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { authStorage } from "@/lib/auth";
import { formatDate } from "@/lib/utils";
import { 
  Bell, 
  Check, 
  CheckCheck, 
  Sparkles, 
  Calendar, 
  Ticket, 
  AlertCircle,
  ExternalLink 
} from "lucide-react";

interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  linkUrl?: string;
  read: boolean;
  createdAt: string;
}

export function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const fetchUnreadCount = async () => {
    const user = authStorage.getUser();
    if (!user) return;

    try {
      const response = await apiClient.get<any>("/notifications/unread-count");
      if (response.data.success && response.data.data) {
        setUnreadCount(response.data.data.unreadCount || 0);
      }
    } catch {
      // Ignore preview fallback
    }
  };

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get<any>("/notifications?size=6");
      if (response.data.success && response.data.data) {
        setNotifications(response.data.data.content || []);
      }
    } catch {
      setNotifications([
        {
          id: "n1",
          type: "REGISTRATION_CONFIRMED",
          title: "Registration Confirmed!",
          message: "You have secured ticket CP-CLEANR-297650 for River Clean-Up.",
          linkUrl: "/my-events",
          read: false,
          createdAt: "2026-08-20T10:00:00Z"
        },
        {
          id: "n2",
          type: "ANNOUNCEMENT",
          title: "Organizer Announcement",
          message: "Meeting point moved to Pavilion #2 due to trail maintenance.",
          linkUrl: "/events/clean-rivers-clean-up",
          read: true,
          createdAt: "2026-08-21T08:00:00Z"
        }
      ]);
      setUnreadCount(1);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleToggle = () => {
    if (!open) {
      fetchNotifications();
    }
    setOpen(!open);
  };

  const handleMarkRead = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await apiClient.patch(`/notifications/${id}/read`);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch {
      // Ignored
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await apiClient.post("/notifications/mark-all-read");
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch {
      // Ignored
    }
  };

  return (
    <div className="relative">
      <Button
        variant="ghost"
        size="icon"
        onClick={handleToggle}
        className="relative h-9 w-9 rounded-xl hover:bg-muted text-muted-foreground hover:text-foreground"
        title="Notifications"
      >
        <Bell className="h-4 w-4" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 h-4 min-w-[16px] px-1 rounded-full bg-emerald-500 text-white text-[10px] font-bold flex items-center justify-center shadow-sm animate-pulse">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </Button>

      {open && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
          <div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl bg-card border border-border shadow-2xl z-50 overflow-hidden animate-in fade-in zoom-in-95">
            <div className="p-4 border-b flex items-center justify-between bg-muted/30">
              <div className="flex items-center gap-2">
                <span className="font-bold text-sm text-foreground">Notifications</span>
                {unreadCount > 0 && (
                  <Badge variant="success" className="text-[10px] px-1.5 py-0">
                    {unreadCount} New
                  </Badge>
                )}
              </div>

              {unreadCount > 0 && (
                <button
                  onClick={handleMarkAllRead}
                  className="text-xs text-primary hover:underline font-semibold flex items-center gap-1"
                >
                  <CheckCheck className="h-3.5 w-3.5" />
                  Mark all read
                </button>
              )}
            </div>

            <div className="max-h-80 overflow-y-auto divide-y divide-border">
              {loading ? (
                <div className="p-8 text-center text-xs text-muted-foreground">Loading...</div>
              ) : notifications.length === 0 ? (
                <div className="p-8 text-center text-xs text-muted-foreground space-y-1">
                  <Bell className="h-6 w-6 mx-auto text-muted-foreground/60 mb-2" />
                  <p className="font-semibold">All caught up!</p>
                  <p>No new notifications right now.</p>
                </div>
              ) : (
                notifications.map((n) => (
                  <div
                    key={n.id}
                    className={`p-3.5 flex items-start gap-3 transition-colors ${
                      n.read ? "bg-card opacity-80" : "bg-primary/5 font-medium"
                    }`}
                  >
                    <div className="p-1.5 rounded-lg bg-primary/10 text-primary shrink-0 mt-0.5">
                      {n.type.includes("REGISTRATION") ? <Ticket className="h-3.5 w-3.5" /> : <Calendar className="h-3.5 w-3.5" />}
                    </div>

                    <div className="flex-1 space-y-0.5 min-w-0">
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-bold text-xs text-foreground truncate">{n.title}</span>
                        <span className="text-[10px] text-muted-foreground shrink-0">{formatDate(n.createdAt)}</span>
                      </div>
                      <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">{n.message}</p>
                      {n.linkUrl && (
                        <Link
                          href={n.linkUrl}
                          onClick={() => setOpen(false)}
                          className="inline-flex items-center gap-1 text-[11px] text-primary hover:underline font-semibold pt-1"
                        >
                          View Details
                          <ExternalLink className="h-2.5 w-2.5" />
                        </Link>
                      )}
                    </div>

                    {!n.read && (
                      <button
                        onClick={(e) => handleMarkRead(n.id, e)}
                        className="h-5 w-5 rounded-full hover:bg-muted text-muted-foreground hover:text-primary flex items-center justify-center shrink-0"
                        title="Mark as read"
                      >
                        <Check className="h-3 w-3" />
                      </button>
                    )}
                  </div>
                ))
              )}
            </div>

            <div className="p-3 bg-muted/20 border-t text-center">
              <Link
                href="/notifications"
                onClick={() => setOpen(false)}
                className="text-xs text-primary font-semibold hover:underline"
              >
                View all notifications in center →
              </Link>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
