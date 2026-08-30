"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { EventItem, ApiResponse, PagedResponse } from "@/types";
import { formatDate } from "@/lib/utils";
import { 
  CalendarDays, 
  PlusCircle, 
  Users, 
  MapPin, 
  Globe, 
  Edit3, 
  CheckCircle, 
  XCircle, 
  QrCode, 
  BarChart3, 
  Sparkles,
  ArrowRight,
  Clock
} from "lucide-react";

export default function ManageEventsPage() {
  const [events, setEvents] = useState<EventItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

  const fetchManageableEvents = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get<ApiResponse<PagedResponse<EventItem>>>("/events/manage");
      if (response.data.success && response.data.data) {
        setEvents(response.data.data.content || []);
      }
    } catch {
      // Fallback demo organizer events
      setEvents([
        {
          id: "e1",
          organizationId: "b1",
          organizationName: "Green Earth Volunteers",
          createdByUserId: "u1",
          categoryId: "c1",
          categoryName: "Environment & Sustainability",
          title: "Clean Rivers Community Clean-Up & Tree Drive",
          slug: "clean-rivers-clean-up",
          description: "Grassroots river cleaning drive at the metropolis riverside.",
          status: "REGISTRATION_OPEN",
          visibility: "PUBLIC",
          startTime: "2026-09-12T09:00:00Z",
          endTime: "2026-09-12T13:00:00Z",
          registrationDeadline: "2026-09-11T18:00:00Z",
          capacity: 100,
          currentRegistrationCount: 86,
          locationType: "IN_PERSON",
          venueName: "Metropolis Riverside Park",
          city: "Metropolis",
          waitlistEnabled: true,
          waitlistCapacity: 50,
          createdAt: "2026-08-01T10:00:00Z",
          updatedAt: "2026-08-01T10:00:00Z"
        },
        {
          id: "e2",
          organizationId: "b1",
          organizationName: "Green Earth Volunteers",
          createdByUserId: "u1",
          categoryId: "c1",
          categoryName: "Environment & Sustainability",
          title: "Urban Tree Plantation Weekend Sprint",
          slug: "urban-tree-plantation-sprint",
          description: "Planting 500 saplings in the urban green belt.",
          status: "DRAFT",
          visibility: "PUBLIC",
          startTime: "2026-09-26T08:00:00Z",
          endTime: "2026-09-26T12:00:00Z",
          registrationDeadline: "2026-09-25T18:00:00Z",
          capacity: 50,
          currentRegistrationCount: 0,
          locationType: "IN_PERSON",
          venueName: "Northside Community Forest",
          city: "Metropolis",
          waitlistEnabled: true,
          waitlistCapacity: 30,
          createdAt: "2026-08-10T10:00:00Z",
          updatedAt: "2026-08-10T10:00:00Z"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchManageableEvents();
  }, []);

  const handlePublish = async (eventId: string) => {
    try {
      await apiClient.post(`/events/${eventId}/publish`);
      fetchManageableEvents();
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to publish event.");
    }
  };

  const handleCancel = async (eventId: string) => {
    if (!confirm("Are you sure you want to cancel this event? This will notify all registered attendees.")) return;
    try {
      await apiClient.post(`/events/${eventId}/cancel`);
      fetchManageableEvents();
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to cancel event.");
    }
  };

  const filteredEvents = statusFilter === "ALL" 
    ? events 
    : events.filter(e => e.status === statusFilter);

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-8 max-w-6xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/40 pb-6">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">Organizer Event Studio</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Create, publish, and oversee registrations, attendance, and analytics for your community events.
          </p>
        </div>

        <Link href="/manage-events/create">
          <Button variant="gradient" size="sm" className="gap-2 font-medium">
            <PlusCircle className="h-4 w-4" />
            Create Event
          </Button>
        </Link>
      </div>

      {/* Status Filter Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2">
        {["ALL", "DRAFT", "PUBLISHED", "REGISTRATION_OPEN", "COMPLETED", "CANCELLED"].map((st) => (
          <button
            key={st}
            onClick={() => setStatusFilter(st)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              statusFilter === st
                ? "bg-primary text-primary-foreground shadow-sm"
                : "bg-muted text-muted-foreground hover:text-foreground"
            }`}
          >
            {st.replace("_", " ")}
          </button>
        ))}
      </div>

      {/* Events List */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2].map((i) => (
            <div key={i} className="h-40 rounded-xl bg-muted/40 animate-pulse" />
          ))}
        </div>
      ) : filteredEvents.length === 0 ? (
        <div className="p-12 text-center border rounded-2xl bg-card space-y-3">
          <CalendarDays className="h-10 w-10 text-muted-foreground mx-auto" />
          <h3 className="text-base font-semibold">No events matching this filter</h3>
          <p className="text-xs text-muted-foreground">Ready to mobilize your community? Create your first event drive.</p>
          <Link href="/manage-events/create">
            <Button variant="gradient" size="sm" className="mt-2">
              Create Event
            </Button>
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredEvents.map((event) => (
            <Card key={event.id} className="border-border/80 hover:border-border transition-all">
              <CardContent className="p-6">
                <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
                  {/* Left: Info */}
                  <div className="space-y-2 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant={
                        event.status === "REGISTRATION_OPEN" ? "success" :
                        event.status === "PUBLISHED" ? "info" :
                        event.status === "DRAFT" ? "secondary" :
                        event.status === "COMPLETED" ? "outline" : "destructive"
                      }>
                        {event.status.replace("_", " ")}
                      </Badge>
                      <span className="text-xs text-muted-foreground font-medium">
                        {event.organizationName} • {event.categoryName}
                      </span>
                    </div>

                    <h3 className="text-lg font-bold text-foreground hover:text-primary transition-colors">
                      <Link href={`/events/${event.slug || event.id}`}>{event.title}</Link>
                    </h3>

                    <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
                      <span className="flex items-center gap-1.5">
                        <Clock className="h-3.5 w-3.5 text-primary" />
                        {formatDate(event.startTime)}
                      </span>
                      <span className="flex items-center gap-1.5">
                        {event.locationType === "VIRTUAL" ? (
                          <Globe className="h-3.5 w-3.5 text-primary" />
                        ) : (
                          <MapPin className="h-3.5 w-3.5 text-primary" />
                        )}
                        <span>{event.venueName || event.city || "Online"}</span>
                      </span>
                      <span className="flex items-center gap-1.5">
                        <Users className="h-3.5 w-3.5 text-primary" />
                        <span>{event.currentRegistrationCount} / {event.capacity} registered</span>
                      </span>
                    </div>

                    {/* Capacity Progress Bar */}
                    <div className="w-full max-w-md pt-2">
                      <div className="w-full bg-muted rounded-full h-1.5 overflow-hidden">
                        <div 
                          className="bg-emerald-500 h-full rounded-full transition-all"
                          style={{ width: `${Math.min(100, (event.currentRegistrationCount / event.capacity) * 100)}%` }}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Right: Actions */}
                  <div className="flex flex-wrap items-center gap-2 shrink-0">
                    {event.status === "DRAFT" && (
                      <Button size="sm" variant="gradient" className="gap-1.5 text-xs" onClick={() => handlePublish(event.id)}>
                        <CheckCircle className="h-3.5 w-3.5" />
                        Publish Event
                      </Button>
                    )}

                    <Link href={`/manage-events/${event.id}/edit`}>
                      <Button size="sm" variant="outline" className="gap-1.5 text-xs">
                        <Edit3 className="h-3.5 w-3.5" />
                        Edit
                      </Button>
                    </Link>

                    {event.status !== "DRAFT" && event.status !== "CANCELLED" && (
                      <>
                        <Link href={`/manage-events/${event.id}/checkin`}>
                          <Button size="sm" variant="secondary" className="gap-1.5 text-xs">
                            <QrCode className="h-3.5 w-3.5 text-primary" />
                            QR Check-In
                          </Button>
                        </Link>

                        <Link href={`/manage-events/${event.id}/analytics`}>
                          <Button size="sm" variant="secondary" className="gap-1.5 text-xs">
                            <BarChart3 className="h-3.5 w-3.5 text-primary" />
                            Analytics
                          </Button>
                        </Link>
                      </>
                    )}

                    {event.status !== "CANCELLED" && event.status !== "COMPLETED" && (
                      <Button size="sm" variant="ghost" className="text-xs text-destructive hover:bg-destructive/10" onClick={() => handleCancel(event.id)}>
                        <XCircle className="h-3.5 w-3.5" />
                      </Button>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
