"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { authStorage } from "@/lib/auth";
import { apiClient } from "@/lib/api-client";
import { User, RegistrationDto, WaitlistEntryDto, ApiResponse } from "@/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { formatDate } from "@/lib/utils";
import { 
  Ticket, 
  CalendarDays, 
  PlusCircle, 
  ShieldCheck, 
  Building2, 
  Sparkles, 
  ArrowRight, 
  Hourglass, 
  Activity, 
  Users, 
  QrCode,
  Compass,
  CheckCircle2,
  ExternalLink
} from "lucide-react";

export default function DashboardPage() {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [registrations, setRegistrations] = useState<RegistrationDto[]>([]);
  const [waitlists, setWaitlists] = useState<WaitlistEntryDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const user = authStorage.getUser();
    setCurrentUser(user);

    const fetchSummary = async () => {
      if (!user) {
        setLoading(false);
        return;
      }
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
        // Fallback for offline preview
        setRegistrations([]);
        setWaitlists([]);
      } finally {
        setLoading(false);
      }
    };

    fetchSummary();
  }, []);

  const confirmedPasses = registrations.filter((r) => r.status === "CONFIRMED");
  const isOrganizerOrAdmin = currentUser && ["ORGANIZER", "ADMIN"].includes(currentUser.role);
  const isModeratorOrAdmin = currentUser && ["MODERATOR", "ADMIN"].includes(currentUser.role);

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-10 max-w-6xl">
      {/* Header Banner */}
      <div className="rounded-3xl bg-gradient-to-tr from-card via-background to-emerald-950/20 border border-border/80 shadow-xl p-8 sm:p-10 relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
              <Badge variant="outline" className="text-xs font-mono font-semibold">
                {currentUser?.role || "MEMBER"} COMMAND CENTER
              </Badge>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-foreground">
              Welcome back, {currentUser?.fullName || currentUser?.email || "Citizen"}
            </h1>
            <p className="text-sm text-muted-foreground max-w-xl">
              Track your dynamic admission QR passes, upcoming civic drives, and organization responsibilities in one place.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Link href="/events">
              <Button variant="outline" size="sm" className="gap-2 text-xs">
                <CalendarDays className="h-4 w-4" />
                Browse Events
              </Button>
            </Link>
            <Link href="/my-events">
              <Button variant="gradient" size="sm" className="gap-2 text-xs shadow-md">
                <Ticket className="h-4 w-4" />
                My Event Passes ({confirmedPasses.length})
              </Button>
            </Link>
          </div>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <Card className="border-border glow-card bg-card/80 backdrop-blur-sm">
          <CardContent className="p-6 flex items-center gap-4">
            <div className="p-3 rounded-2xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
              <Ticket className="h-6 w-6" />
            </div>
            <div>
              <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">Confirmed Passes</span>
              <span className="text-2xl font-black text-foreground">{loading ? "..." : confirmedPasses.length}</span>
            </div>
          </CardContent>
        </Card>

        <Card className="border-border glow-card bg-card/80 backdrop-blur-sm">
          <CardContent className="p-6 flex items-center gap-4">
            <div className="p-3 rounded-2xl bg-amber-500/10 text-amber-600 dark:text-amber-400">
              <Hourglass className="h-6 w-6" />
            </div>
            <div>
              <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">Waitlist Queues</span>
              <span className="text-2xl font-black text-foreground">{loading ? "..." : waitlists.length}</span>
            </div>
          </CardContent>
        </Card>

        <Card className="border-border glow-card bg-card/80 backdrop-blur-sm">
          <CardContent className="p-6 flex items-center gap-4">
            <div className="p-3 rounded-2xl bg-primary/10 text-primary">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <div>
              <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">Dynamic QR Guard</span>
              <span className="text-sm font-bold text-emerald-500">Active (120s TTL)</span>
            </div>
          </CardContent>
        </Card>

        <Card className="border-border glow-card bg-card/80 backdrop-blur-sm">
          <CardContent className="p-6 flex items-center gap-4">
            <div className="p-3 rounded-2xl bg-teal-500/10 text-teal-600 dark:text-teal-400">
              <Activity className="h-6 w-6" />
            </div>
            <div>
              <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider block">System Status</span>
              <span className="text-sm font-bold text-emerald-500">100% Operational</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Quick Navigation Cards */}
      <div className="space-y-4">
        <h2 className="text-xl font-bold tracking-tight text-foreground flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-primary" />
          Platform Hub & Tools
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Member Pass Hub */}
          <Card className="border-border/80 bg-card hover:border-primary/50 transition-colors flex flex-col justify-between">
            <CardHeader className="pb-3">
              <div className="p-2.5 rounded-xl bg-primary/10 text-primary w-fit mb-2">
                <QrCode className="h-5 w-5" />
              </div>
              <CardTitle className="text-base font-bold">My Admission Passes</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-xs text-muted-foreground">
              <p>View your HMAC-SHA256 dynamic tickets, download passes, and check in at events.</p>
              <Link href="/my-events">
                <Button variant="outline" size="sm" className="w-full gap-1.5 text-xs">
                  Open Passes Hub <ArrowRight className="h-3.5 w-3.5" />
                </Button>
              </Link>
            </CardContent>
          </Card>

          {/* Event Discovery */}
          <Card className="border-border/80 bg-card hover:border-primary/50 transition-colors flex flex-col justify-between">
            <CardHeader className="pb-3">
              <div className="p-2.5 rounded-xl bg-teal-500/10 text-teal-500 w-fit mb-2">
                <Compass className="h-5 w-5" />
              </div>
              <CardTitle className="text-base font-bold">Discover Events</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-xs text-muted-foreground">
              <p>Explore river cleanups, hackathons, open data forums, and tree plantation drives.</p>
              <Link href="/events">
                <Button variant="outline" size="sm" className="w-full gap-1.5 text-xs">
                  Explore Discovery <ArrowRight className="h-3.5 w-3.5" />
                </Button>
              </Link>
            </CardContent>
          </Card>

          {/* Organizer / Admin Console */}
          {isOrganizerOrAdmin ? (
            <Card className="border-border/80 bg-card hover:border-primary/50 transition-colors flex flex-col justify-between">
              <CardHeader className="pb-3">
                <div className="p-2.5 rounded-xl bg-emerald-500/10 text-emerald-500 w-fit mb-2">
                  <PlusCircle className="h-5 w-5" />
                </div>
                <CardTitle className="text-base font-bold">Host & Manage Events</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4 text-xs text-muted-foreground">
                <p>Create community drives, scan attendee QR codes, and view real-time attendance telemetry.</p>
                <Link href="/manage-events">
                  <Button variant="outline" size="sm" className="w-full gap-1.5 text-xs">
                    Manage Events <ArrowRight className="h-3.5 w-3.5" />
                  </Button>
                </Link>
              </CardContent>
            </Card>
          ) : (
            <Card className="border-border/80 bg-card hover:border-primary/50 transition-colors flex flex-col justify-between">
              <CardHeader className="pb-3">
                <div className="p-2.5 rounded-xl bg-emerald-500/10 text-emerald-500 w-fit mb-2">
                  <Building2 className="h-5 w-5" />
                </div>
                <CardTitle className="text-base font-bold">Community Organizations</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4 text-xs text-muted-foreground">
                <p>Connect with verified chapters, grassroots charities, and environmental councils.</p>
                <Link href="/organizations">
                  <Button variant="outline" size="sm" className="w-full gap-1.5 text-xs">
                    Browse Organizations <ArrowRight className="h-3.5 w-3.5" />
                  </Button>
                </Link>
              </CardContent>
            </Card>
          )}
        </div>
      </div>

      {/* Confirmed Passes Quick Preview */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold tracking-tight text-foreground flex items-center gap-2">
            <Ticket className="h-5 w-5 text-primary" />
            Active Confirmed Passes
          </h2>
          <Link href="/my-events" className="text-xs font-semibold text-primary hover:underline flex items-center gap-1">
            View All Passes ({confirmedPasses.length}) <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        {loading ? (
          <div className="h-32 rounded-2xl bg-muted/40 animate-pulse border" />
        ) : confirmedPasses.length === 0 ? (
          <div className="p-10 rounded-2xl border bg-card text-center space-y-3">
            <Ticket className="h-8 w-8 text-muted-foreground mx-auto" />
            <h3 className="text-sm font-bold">No active passes yet</h3>
            <p className="text-xs text-muted-foreground">You have not registered for any upcoming events.</p>
            <Link href="/events">
              <Button variant="gradient" size="sm" className="mt-2 text-xs">
                Explore Available Events
              </Button>
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {confirmedPasses.slice(0, 4).map((ticket) => (
              <Card key={ticket.id} className="border-border/80 bg-card p-5 flex items-center justify-between gap-4 glow-card">
                <div className="space-y-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <Badge variant="success" className="text-[10px]">CONFIRMED</Badge>
                    <span className="font-mono text-[11px] font-bold text-primary">{ticket.ticketCode}</span>
                  </div>
                  <h4 className="font-bold text-sm text-foreground truncate">{ticket.eventTitle}</h4>
                  <p className="text-xs text-muted-foreground">Date: {formatDate(ticket.startTime)}</p>
                </div>
                <Link href="/my-events">
                  <Button variant="outline" size="sm" className="text-xs shrink-0 gap-1.5">
                    <QrCode className="h-3.5 w-3.5" />
                    QR Pass
                  </Button>
                </Link>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
