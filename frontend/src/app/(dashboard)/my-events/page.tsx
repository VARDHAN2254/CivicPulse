"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { TicketCard } from "@/components/events/TicketCard";
import { apiClient } from "@/lib/api-client";
import { RegistrationDto, WaitlistEntryDto, ApiResponse } from "@/types";
import { formatDate } from "@/lib/utils";
import { 
  Ticket, 
  Hourglass, 
  History, 
  CalendarDays, 
  ArrowRight, 
  CheckCircle2, 
  ShieldCheck, 
  Sparkles 
} from "lucide-react";

export default function MyEventsPage() {
  const [registrations, setRegistrations] = useState<RegistrationDto[]>([]);
  const [waitlists, setWaitlists] = useState<WaitlistEntryDto[]>([]);
  const [activeTab, setActiveTab] = useState<"CONFIRMED" | "WAITLIST" | "PAST">("CONFIRMED");
  const [loading, setLoading] = useState(true);
  const [successNotice, setSuccessNotice] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [regRes, waitRes] = await Promise.all([
        apiClient.get<ApiResponse<RegistrationDto[]>>("/registrations/my"),
        apiClient.get<ApiResponse<WaitlistEntryDto[]>>("/registrations/my-waitlists"),
      ]);

      if (regRes.data.success && regRes.data.data) {
        setRegistrations(regRes.data.data);
      }
      if (waitRes.data.success && waitRes.data.data) {
        setWaitlists(waitRes.data.data);
      }
    } catch {
      // Fallback demo tickets
      setRegistrations([
        {
          id: "7816f563-82a1-41a8-8911-2e512410e42a",
          ticketCode: "CP-CLEANR-297650",
          eventId: "f0000000-0000-0000-0000-000000000001",
          eventTitle: "Clean Rivers Community Clean-Up & Tree Drive",
          eventSlug: "clean-rivers-clean-up",
          startTime: "2026-10-12T09:00:00Z",
          endTime: "2026-10-12T13:00:00Z",
          venueName: "Metropolis Riverside Park - Pavilion #2",
          organizationName: "Green Earth Volunteers",
          userId: "a0000000-0000-0000-0000-000000000004",
          userEmail: "member@civicpulse.org",
          userFullName: "Alex Rivera (Member)",
          status: "CONFIRMED",
          registeredAt: "2026-08-20T10:00:00Z"
        }
      ]);
      setWaitlists([
        {
          id: "6816f563-82a1-41a8-8911-2e512410e42b",
          eventId: "f0000000-0000-0000-0000-000000000002",
          eventTitle: "Metropolis AI & Civic Tech Hackathon 2026",
          eventSlug: "metropolis-hackathon-2026",
          startTime: "2026-10-24T10:00:00Z",
          organizationName: "Metropolis Tech Council",
          userId: "a0000000-0000-0000-0000-000000000004",
          userEmail: "member@civicpulse.org",
          userFullName: "Alex Rivera (Member)",
          position: 2,
          status: "PENDING",
          joinedAt: "2026-08-22T14:00:00Z"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCancelRegistration = async (registrationId: string) => {
    try {
      const res = await apiClient.post(`/registrations/${registrationId}/cancel`);
      if (res.data.success) {
        setSuccessNotice("Ticket cancelled. Your slot was offered to the next waitlisted volunteer.");
        fetchData();
      }
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to cancel ticket pass.");
    }
  };

  const confirmedPasses = registrations.filter(r => r.status === "CONFIRMED");
  const pastPasses = registrations.filter(r => r.status !== "CONFIRMED");

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-8 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/40 pb-6">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">My Events & Ticket Passes</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Access your event admission QR passes, check waitlist status, and manage reservations.
          </p>
        </div>

        <Link href="/events">
          <Button variant="gradient" size="sm" className="gap-2">
            <CalendarDays className="h-4 w-4" />
            Discover More Events
          </Button>
        </Link>
      </div>

      {successNotice && (
        <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{successNotice}</span>
        </div>
      )}

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b pb-3">
        <button
          onClick={() => setActiveTab("CONFIRMED")}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === "CONFIRMED"
              ? "bg-primary text-primary-foreground shadow-sm"
              : "bg-muted text-muted-foreground hover:text-foreground"
          }`}
        >
          <Ticket className="h-3.5 w-3.5" />
          <span>Confirmed Passes ({confirmedPasses.length})</span>
        </button>

        <button
          onClick={() => setActiveTab("WAITLIST")}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === "WAITLIST"
              ? "bg-primary text-primary-foreground shadow-sm"
              : "bg-muted text-muted-foreground hover:text-foreground"
          }`}
        >
          <Hourglass className="h-3.5 w-3.5" />
          <span>Waitlist Queue ({waitlists.length})</span>
        </button>

        <button
          onClick={() => setActiveTab("PAST")}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === "PAST"
              ? "bg-primary text-primary-foreground shadow-sm"
              : "bg-muted text-muted-foreground hover:text-foreground"
          }`}
        >
          <History className="h-3.5 w-3.5" />
          <span>History & Cancelled ({pastPasses.length})</span>
        </button>
      </div>

      {/* Tab Content */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="h-56 rounded-2xl bg-muted/40 animate-pulse border" />
          <div className="h-56 rounded-2xl bg-muted/40 animate-pulse border" />
        </div>
      ) : activeTab === "CONFIRMED" ? (
        confirmedPasses.length === 0 ? (
          <div className="p-16 text-center border rounded-2xl bg-card space-y-4">
            <Ticket className="h-10 w-10 text-muted-foreground mx-auto" />
            <h3 className="text-base font-bold">No active confirmed passes</h3>
            <p className="text-xs text-muted-foreground">You haven&apos;t reserved any event tickets yet.</p>
            <Link href="/events">
              <Button variant="gradient" size="sm" className="mt-2">
                Explore Upcoming Drives
              </Button>
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {confirmedPasses.map((ticket) => (
              <TicketCard key={ticket.id} ticket={ticket} onCancel={handleCancelRegistration} />
            ))}
          </div>
        )
      ) : activeTab === "WAITLIST" ? (
        waitlists.length === 0 ? (
          <div className="p-16 text-center border rounded-2xl bg-card space-y-3">
            <Hourglass className="h-10 w-10 text-muted-foreground mx-auto" />
            <h3 className="text-base font-bold">No active waitlists</h3>
            <p className="text-xs text-muted-foreground">You are currently not on any event waitlists.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {waitlists.map((entry) => (
              <Card key={entry.id} className="border-border p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <Badge variant="warning">Waitlist Position #{entry.position}</Badge>
                    <span className="text-xs text-muted-foreground font-medium">{entry.organizationName}</span>
                  </div>
                  <h3 className="font-bold text-base text-foreground">
                    <Link href={`/events/${entry.eventSlug || entry.eventId}`} className="hover:text-primary transition-colors">
                      {entry.eventTitle}
                    </Link>
                  </h3>
                  <p className="text-xs text-muted-foreground">
                    Event Date: {formatDate(entry.startTime)} • Joined: {formatDate(entry.joinedAt)}
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <Link href={`/events/${entry.eventSlug || entry.eventId}`}>
                    <Button variant="outline" size="sm" className="text-xs">
                      View Event
                    </Button>
                  </Link>
                </div>
              </Card>
            ))}
          </div>
        )
      ) : (
        pastPasses.length === 0 ? (
          <div className="p-16 text-center border rounded-2xl bg-card space-y-3">
            <History className="h-10 w-10 text-muted-foreground mx-auto" />
            <h3 className="text-base font-bold">No past registrations</h3>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {pastPasses.map((ticket) => (
              <TicketCard key={ticket.id} ticket={ticket} />
            ))}
          </div>
        )
      )}
    </div>
  );
}
