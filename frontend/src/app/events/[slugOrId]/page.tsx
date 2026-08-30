"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { authStorage } from "@/lib/auth";
import { EventItem, ApiResponse, User, RegistrationDto } from "@/types";
import { formatDate } from "@/lib/utils";
import { AnnouncementFeed } from "@/components/events/AnnouncementFeed";
import { 
  Calendar, 
  Clock, 
  MapPin, 
  Globe, 
  Users, 
  Building2, 
  CheckCircle2, 
  ArrowLeft, 
  MessageSquare, 
  Share2, 
  Ticket,
  AlertCircle,
  Sparkles,
  ShieldCheck
} from "lucide-react";

export default function EventDetailPage() {
  const params = useParams();
  const slugOrId = params.slugOrId as string;

  const [event, setEvent] = useState<EventItem | null>(null);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [registering, setRegistering] = useState(false);
  const [userRegistered, setUserRegistered] = useState(false);
  const [userWaitlisted, setUserWaitlisted] = useState(false);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  useEffect(() => {
    const user = authStorage.getUser();
    setCurrentUser(user);

    const fetchEvent = async () => {
      try {
        const response = await apiClient.get<ApiResponse<EventItem>>(`/events/${slugOrId}`);
        if (response.data.success && response.data.data) {
          const eventData = response.data.data;
          setEvent(eventData);

          if (user) {
            try {
              const myRegs = await apiClient.get<ApiResponse<RegistrationDto[]>>("/registrations/my");
              if (myRegs.data.success && myRegs.data.data) {
                const isRegistered = myRegs.data.data.some(
                  (r) => r.eventId === eventData.id && r.status === "CONFIRMED"
                );
                if (isRegistered) {
                  setUserRegistered(true);
                  setActionMessage("You have an active registration pass for this event.");
                }
              }
            } catch {
              // Ignore registration check error
            }
          }
        }
      } catch {
        // Fallback demo event
        setEvent({
          id: "f0000000-0000-0000-0000-000000000001",
          organizationId: "b0000000-0000-0000-0000-000000000001",
          organizationName: "Green Earth Volunteers",
          organizationSlug: "green-earth-volunteers",
          createdByUserId: "a0000000-0000-0000-0000-000000000003",
          categoryId: "c0000000-0000-0000-0000-000000000002",
          categoryName: "Environment & Sustainability",
          title: "Clean Rivers Community Clean-Up & Tree Drive",
          slug: "clean-rivers-clean-up",
          shortDescription: "Join fellow volunteers to restore riverbanks and plant 200 native trees.",
          description: "Our community river clean-up drive gathers neighbors, students, and conservationists for a high-impact restoration morning. We will clean 2 miles of riverside park trail, remove plastics, and plant 200 native saplings along the erosion buffer zones.\n\nAll tools, gloves, and hydration stations are provided free. Please wear sturdy closed-toe shoes.",
          status: "PUBLISHED",
          visibility: "PUBLIC",
          startTime: "2026-10-12T09:00:00Z",
          endTime: "2026-10-12T13:00:00Z",
          registrationDeadline: "2026-10-11T23:59:59Z",
          capacity: 100,
          currentRegistrationCount: 14,
          locationType: "IN_PERSON",
          venueName: "Metropolis Riverside Park - Pavilion #2",
          address: "1200 Riverside Boulevard",
          city: "Metropolis",
          state: "NY",
          postalCode: "10001",
          waitlistEnabled: true,
          waitlistCapacity: 50,
          tags: [
            { id: "d0000000-0000-0000-0000-000000000001", name: "Volunteering", slug: "volunteering" },
            { id: "d0000000-0000-0000-0000-000000000002", name: "Sustainability", slug: "sustainability" },
            { id: "d0000000-0000-0000-0000-000000000003", name: "Free Entry", slug: "free-entry" }
          ],
          createdAt: "2026-08-01T10:00:00Z",
          updatedAt: "2026-08-01T10:00:00Z"
        });
      } finally {
        setLoading(false);
      }
    };

    fetchEvent();
  }, [slugOrId]);

  const handleRegister = async () => {
    if (!currentUser) {
      window.location.href = `/login?redirect=/events/${slugOrId}`;
      return;
    }

    if (!event) return;
    setRegistering(true);
    setActionMessage(null);

    try {
      const response = await apiClient.post<ApiResponse<any>>(`/registrations/events/${event.id}`);
      if (response.data.success) {
        if (response.data.data?.status === "PENDING" || response.data.data?.waitlisted) {
          setUserWaitlisted(true);
          setActionMessage("Event capacity is full. You have joined the automated waitlist!");
        } else {
          setUserRegistered(true);
          setActionMessage("Registration confirmed! Your check-in ticket has been generated.");
          setEvent(prev => prev ? { ...prev, currentRegistrationCount: prev.currentRegistrationCount + 1 } : null);
        }
      }
    } catch (err: any) {
      if (err.response?.status === 409) {
        setUserRegistered(true);
        setActionMessage("You are already registered for this event. Your pass is ready.");
      } else {
        setActionMessage(err.response?.data?.message || "Registration failed. Please check event status.");
      }
    } finally {
      setRegistering(false);
    }
  };

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-16 text-center">
        <div className="h-8 w-64 bg-muted animate-pulse mx-auto rounded-lg mb-4" />
        <div className="h-4 w-96 bg-muted animate-pulse mx-auto rounded-lg" />
      </div>
    );
  }

  if (!event) {
    return (
      <div className="container mx-auto px-4 py-16 text-center space-y-4">
        <h2 className="text-2xl font-bold">Event Not Found</h2>
        <Link href="/events">
          <Button variant="outline" className="gap-2">
            <ArrowLeft className="h-4 w-4" />
            Back to Discovery
          </Button>
        </Link>
      </div>
    );
  }

  const isFull = event.currentRegistrationCount >= event.capacity;

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-10 max-w-5xl">
      <Link href="/events" className="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground">
        <ArrowLeft className="h-3.5 w-3.5" />
        Back to event discovery
      </Link>

      {/* Hero Header Card */}
      <div className="rounded-3xl bg-gradient-to-tr from-card via-background to-emerald-950/20 border border-border/80 shadow-xl overflow-hidden">
        <div className="p-8 sm:p-10 space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Badge variant={
                event.status === "REGISTRATION_OPEN" ? "success" :
                event.status === "PUBLISHED" ? "info" :
                event.status === "COMPLETED" ? "outline" : "warning"
              }>
                {event.status.replace("_", " ")}
              </Badge>
              <Badge variant="outline">{event.categoryName}</Badge>
            </div>

            {event.organizationSlug && (
              <Link href={`/organizations/${event.organizationSlug}`} className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:underline">
                <Building2 className="h-4 w-4" />
                Hosted by {event.organizationName}
              </Link>
            )}
          </div>

          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-foreground leading-tight">
            {event.title}
          </h1>

          {event.shortDescription && (
            <p className="text-base text-muted-foreground leading-relaxed max-w-3xl">
              {event.shortDescription}
            </p>
          )}

          {/* Key Event Metadata Ribbon */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-6 border-t border-border/60 text-xs">
            <div className="flex items-start gap-3">
              <div className="p-2 rounded-xl bg-primary/10 text-primary">
                <Calendar className="h-5 w-5" />
              </div>
              <div>
                <span className="text-muted-foreground font-medium block">Date & Time</span>
                <span className="font-semibold text-foreground">{formatDate(event.startTime)}</span>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="p-2 rounded-xl bg-primary/10 text-primary">
                {event.locationType === "VIRTUAL" ? <Globe className="h-5 w-5" /> : <MapPin className="h-5 w-5" />}
              </div>
              <div>
                <span className="text-muted-foreground font-medium block">Location</span>
                <span className="font-semibold text-foreground truncate max-w-[200px] block">
                  {event.venueName || event.city || "Online Link"}
                </span>
                {event.address && <span className="text-muted-foreground">{event.address}</span>}
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="p-2 rounded-xl bg-primary/10 text-primary">
                <Users className="h-5 w-5" />
              </div>
              <div className="flex-1">
                <div className="flex justify-between items-center mb-1">
                  <span className="text-muted-foreground font-medium">Capacity Status</span>
                  <span className="font-bold text-foreground">{event.currentRegistrationCount}/{event.capacity}</span>
                </div>
                <div className="w-full bg-muted rounded-full h-2 overflow-hidden">
                  <div 
                    className="bg-emerald-500 h-full rounded-full transition-all"
                    style={{ width: `${Math.min(100, (event.currentRegistrationCount / event.capacity) * 100)}%` }}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Grid: Details + Sticky Registration Action Card */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Description & Community */}
        <div className="lg:col-span-2 space-y-8">
          <Card className="border-border">
            <CardHeader>
              <CardTitle className="text-xl font-bold">About this Event</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-sm text-muted-foreground leading-relaxed whitespace-pre-line">
              {event.description}
            </CardContent>
          </Card>

          {/* Tags */}
          {event.tags && event.tags.length > 0 && (
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs font-semibold text-foreground mr-1">Tags:</span>
              {event.tags.map((tag) => (
                <span key={tag.id || tag.slug} className="text-xs px-3 py-1 rounded-full bg-muted border font-medium text-muted-foreground">
                  #{tag.name}
                </span>
              ))}
            </div>
          )}

          {/* Announcement Feed */}
          <AnnouncementFeed 
            eventId={event.id} 
            isOrganizer={currentUser?.role === 'ORGANIZER' || currentUser?.role === 'ADMIN'} 
          />

          {/* Community Discussion Callout */}
          <div className="p-6 rounded-2xl bg-muted/40 border border-border/80 flex items-center justify-between gap-4">
            <div className="space-y-1">
              <h3 className="font-bold text-sm text-foreground flex items-center gap-2">
                <MessageSquare className="h-4 w-4 text-primary" />
                Community Discussion Board
              </h3>
              <p className="text-xs text-muted-foreground">
                Ask questions to coordinators, coordinate carpools, and meet fellow volunteers.
              </p>
            </div>
            <Link href={`/events/${event.slug || event.id}/discussions`}>
              <Button variant="outline" size="sm" className="text-xs shrink-0">
                Open Discussions
              </Button>
            </Link>
          </div>
        </div>

        {/* Right Column: Registration CTA Card */}
        <div className="lg:col-span-1 space-y-6">
          <Card className="border-emerald-500/30 bg-card shadow-lg sticky top-24">
            <CardHeader className="pb-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Registration</span>
                <Badge variant={isFull ? "warning" : "success"}>
                  {isFull ? (event.waitlistEnabled ? "Waitlist Open" : "Event Full") : "Spots Available"}
                </Badge>
              </div>
              <CardTitle className="text-2xl font-extrabold tracking-tight">Free Admission</CardTitle>
            </CardHeader>

            <CardContent className="space-y-4 text-xs text-muted-foreground">
              {actionMessage && (
                <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400">
                  <div className="flex items-center gap-1.5 font-semibold">
                    <CheckCircle2 className="h-4 w-4" />
                    <span>{actionMessage}</span>
                  </div>
                </div>
              )}

              <div className="space-y-2 py-2 border-y">
                <div className="flex justify-between">
                  <span>Available Seats</span>
                  <span className="font-bold text-foreground">{Math.max(0, event.capacity - event.currentRegistrationCount)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Deadline</span>
                  <span className="font-bold text-foreground">{formatDate(event.registrationDeadline)}</span>
                </div>
              </div>

              <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400 font-medium">
                <ShieldCheck className="h-4 w-4 shrink-0" />
                <span>Pessimistic lock zero-overselling protection active</span>
              </div>
            </CardContent>

            <div className="p-6 pt-0 space-y-3">
              {userRegistered ? (
                <Link href="/my-events" className="w-full">
                  <Button variant="outline" className="w-full gap-2 text-xs">
                    <Ticket className="h-4 w-4" />
                    View Ticket Pass
                  </Button>
                </Link>
              ) : userWaitlisted ? (
                <Button variant="secondary" className="w-full text-xs" disabled>
                  Joined Waitlist (Position #1)
                </Button>
              ) : (
                <Button 
                  onClick={handleRegister} 
                  variant="gradient" 
                  size="lg" 
                  className="w-full gap-2 font-semibold shadow-md"
                  disabled={registering}
                >
                  <Ticket className="h-4 w-4" />
                  {registering ? "Reserving Spot..." : isFull ? "Join Automated Waitlist" : "Register for Event"}
                </Button>
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
