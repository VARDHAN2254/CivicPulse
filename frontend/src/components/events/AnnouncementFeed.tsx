"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { formatDate } from "@/lib/utils";
import { Megaphone, PlusCircle, Send, AlertCircle, Clock, User } from "lucide-react";

interface Announcement {
  id: string;
  eventId: string;
  createdByUserName: string;
  title: string;
  message: string;
  createdAt: string;
}

interface AnnouncementFeedProps {
  eventId: string;
  isOrganizer?: boolean;
}

export function AnnouncementFeed({ eventId, isOrganizer = false }: AnnouncementFeedProps) {
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);
  const [showComposer, setShowComposer] = useState(false);
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAnnouncements = async () => {
    try {
      const response = await apiClient.get<any>(`/events/${eventId}/announcements`);
      if (response.data.success && response.data.data) {
        setAnnouncements(response.data.data);
      }
    } catch {
      // Fallback
      setAnnouncements([
        {
          id: "a1",
          eventId,
          createdByUserName: "Sarah Jenkins (Lead Organizer)",
          title: "Assembly Point & Parking Update",
          message: "Free volunteer parking is reserved in Lot C next to the pavilion. Water stations will be active starting 8:30 AM.",
          createdAt: "2026-08-22T08:00:00Z"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnnouncements();
  }, [eventId]);

  const handlePost = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setPosting(true);

    try {
      const response = await apiClient.post<any>(`/events/${eventId}/announcements`, {
        title: title.trim(),
        message: message.trim(),
      });

      if (response.data.success) {
        setTitle("");
        setMessage("");
        setShowComposer(false);
        fetchAnnouncements();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to post announcement.");
    } finally {
      setPosting(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-base font-bold flex items-center gap-2">
          <Megaphone className="h-4 w-4 text-primary" />
          Organizer Announcements ({announcements.length})
        </h3>

        {isOrganizer && (
          <Button onClick={() => setShowComposer(!showComposer)} variant="outline" size="sm" className="text-xs gap-1.5 h-8">
            <PlusCircle className="h-3.5 w-3.5" />
            {showComposer ? "Close" : "Post Broadcast"}
          </Button>
        )}
      </div>

      {/* Organizer Composer */}
      {showComposer && (
        <Card className="border-emerald-500/40 bg-card shadow-lg animate-in fade-in">
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-bold">Broadcast Urgent Announcement</CardTitle>
            <CardDescription className="text-xs">
              This message will be broadcast live over WebSocket and sent as an in-app notification to all confirmed attendees.
            </CardDescription>
          </CardHeader>

          <form onSubmit={handlePost}>
            <CardContent className="space-y-3">
              {error && (
                <div className="flex items-center gap-1.5 p-2.5 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-xs">
                  <AlertCircle className="h-3.5 w-3.5 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <div className="space-y-1">
                <label className="text-xs font-semibold">Announcement Title</label>
                <Input
                  type="text"
                  required
                  placeholder="e.g. Weather update / Meeting point change"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold">Message Body</label>
                <textarea
                  rows={3}
                  required
                  placeholder="Provide precise instructions or important reminders..."
                  className="flex w-full rounded-lg border border-input bg-background px-3 py-2 text-xs ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button type="button" variant="ghost" size="sm" onClick={() => setShowComposer(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="gradient" size="sm" disabled={posting} className="gap-1.5">
                  <Send className="h-3.5 w-3.5" />
                  {posting ? "Broadcasting..." : "Broadcast Update"}
                </Button>
              </div>
            </CardContent>
          </form>
        </Card>
      )}

      {/* Announcements List */}
      {announcements.length === 0 ? (
        <div className="p-8 text-center border rounded-xl bg-card text-xs text-muted-foreground">
          No announcements posted for this event yet.
        </div>
      ) : (
        <div className="space-y-3">
          {announcements.map((ann) => (
            <Card key={ann.id} className="border-border bg-card">
              <CardContent className="p-4 space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <h4 className="font-bold text-sm text-foreground">{ann.title}</h4>
                  <span className="text-[10px] text-muted-foreground flex items-center gap-1 shrink-0">
                    <Clock className="h-3 w-3" />
                    {formatDate(ann.createdAt)}
                  </span>
                </div>

                <p className="text-xs text-muted-foreground leading-relaxed whitespace-pre-line">
                  {ann.message}
                </p>

                <div className="pt-2 border-t border-border/40 text-[10px] text-muted-foreground flex items-center gap-1 font-medium">
                  <User className="h-3 w-3 text-primary" />
                  <span>Posted by {ann.createdByUserName || "Organizer"}</span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
