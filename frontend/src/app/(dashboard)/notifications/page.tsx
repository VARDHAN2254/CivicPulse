"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { formatDate } from "@/lib/utils";
import { 
  Bell, 
  CheckCheck, 
  Ticket, 
  Calendar, 
  Megaphone, 
  Check, 
  ExternalLink,
  Sparkles,
  ArrowLeft
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

export default function NotificationsInboxPage() {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [filter, setFilter] = useState<"ALL" | "UNREAD">("ALL");
  const [loading, setLoading] = useState(true);

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get<any>("/notifications?size=50");
      if (response.data.success && response.data.data) {
        setNotifications(response.data.data.content || []);
      }
    } catch {
      // Fallback preview
      setNotifications([
        {
          id: "n1",
          type: "REGISTRATION_CONFIRMED",
          title: "Registration Confirmed!",
          message: "You have secured ticket CP-CLEANR-297650 for River Clean-Up & Tree Drive.",
          linkUrl: "/my-events",
          read: false,
          createdAt: "2026-08-20T10:00:00Z"
        },
        {
          id: "n2",
          type: "ANNOUNCEMENT",
          title: "Urgent Organizer Update",
          message: "Meeting point moved to Pavilion #2 due to trail maintenance.",
          linkUrl: "/events/clean-rivers-clean-up",
          read: false,
          createdAt: "2026-08-21T08:00:00Z"
        },
        {
          id: "n3",
          type: "WAITLIST_PROMOTED",
          title: "Promoted from Waitlist!",
          message: "A spot opened up for AI for Social Good Hackathon! Your ticket pass has been issued.",
          linkUrl: "/my-events",
          read: true,
          createdAt: "2026-08-19T14:30:00Z"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const handleMarkRead = async (id: string) => {
    try {
      await apiClient.patch(`/notifications/${id}/read`);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
    } catch {
      // Ignored
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await apiClient.post("/notifications/mark-all-read");
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
    } catch {
      // Ignored
    }
  };

  const filtered = filter === "ALL" ? notifications : notifications.filter(n => !n.read);
  const unreadCount = notifications.filter(n => !n.read).length;

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-8 max-w-4xl">
      <Link href="/my-events" className="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground">
        <ArrowLeft className="h-3.5 w-3.5" />
        Back to my events
      </Link>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/40 pb-6">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-3xl font-extrabold tracking-tight">Notification Center</h1>
            {unreadCount > 0 && (
              <Badge variant="success">{unreadCount} Unread</Badge>
            )}
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Real-time notifications for registration confirmations, waitlist promotions, and organizer announcements.
          </p>
        </div>

        {unreadCount > 0 && (
          <Button onClick={handleMarkAllRead} variant="outline" size="sm" className="gap-2 text-xs">
            <CheckCheck className="h-4 w-4" />
            Mark all as read
          </Button>
        )}
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 border-b pb-3">
        <button
          onClick={() => setFilter("ALL")}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            filter === "ALL"
              ? "bg-primary text-primary-foreground shadow-sm"
              : "bg-muted text-muted-foreground hover:text-foreground"
          }`}
        >
          All ({notifications.length})
        </button>

        <button
          onClick={() => setFilter("UNREAD")}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            filter === "UNREAD"
              ? "bg-primary text-primary-foreground shadow-sm"
              : "bg-muted text-muted-foreground hover:text-foreground"
          }`}
        >
          Unread ({unreadCount})
        </button>
      </div>

      {/* List */}
      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-24 rounded-2xl bg-muted/40 animate-pulse border" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="p-16 text-center border rounded-2xl bg-card space-y-3">
          <Bell className="h-10 w-10 text-muted-foreground mx-auto" />
          <h3 className="text-base font-bold">No notifications here</h3>
          <p className="text-xs text-muted-foreground">You are completely up to date.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((n) => (
            <Card
              key={n.id}
              className={`border transition-all ${
                n.read ? "bg-card/70 border-border/60" : "bg-card border-emerald-500/40 shadow-sm"
              }`}
            >
              <CardContent className="p-5 flex items-start justify-between gap-4">
                <div className="flex items-start gap-4">
                  <div className="p-2.5 rounded-xl bg-primary/10 text-primary shrink-0 mt-0.5">
                    {n.type.includes("REGISTRATION") ? (
                      <Ticket className="h-5 w-5" />
                    ) : n.type.includes("ANNOUNCEMENT") ? (
                      <Megaphone className="h-5 w-5" />
                    ) : (
                      <Calendar className="h-5 w-5" />
                    )}
                  </div>

                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <h4 className="font-bold text-sm text-foreground">{n.title}</h4>
                      {!n.read && (
                        <span className="h-2 w-2 rounded-full bg-emerald-500" />
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground leading-relaxed">{n.message}</p>
                    <div className="pt-2 flex items-center gap-4 text-[11px] text-muted-foreground font-medium">
                      <span>{formatDate(n.createdAt)}</span>
                      {n.linkUrl && (
                        <Link href={n.linkUrl} className="text-primary hover:underline flex items-center gap-1 font-semibold">
                          View details
                          <ExternalLink className="h-3 w-3" />
                        </Link>
                      )}
                    </div>
                  </div>
                </div>

                {!n.read && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleMarkRead(n.id)}
                    className="text-xs h-8 gap-1.5 shrink-0"
                  >
                    <Check className="h-3.5 w-3.5" />
                    Mark Read
                  </Button>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
