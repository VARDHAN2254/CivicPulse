"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { 
  Search, 
  Calendar, 
  MapPin, 
  Users, 
  QrCode, 
  ShieldCheck, 
  TrendingUp, 
  Sparkles, 
  CheckCircle2, 
  ArrowRight,
  Leaf,
  Cpu,
  Palette,
  HeartPulse,
  GraduationCap,
  HandHeart
} from "lucide-react";

export default function HomePage() {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState("");

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/events?query=${encodeURIComponent(searchQuery.trim())}`);
    } else {
      router.push("/events");
    }
  };

  const categories = [
    { name: "Environment & Green", slug: "environment-sustainability", icon: Leaf, count: 12 },
    { name: "Tech & Innovation", slug: "tech-innovation", icon: Cpu, count: 18 },
    { name: "Arts & Culture", slug: "arts-culture", icon: Palette, count: 9 },
    { name: "Health & Wellness", slug: "health-wellness", icon: HeartPulse, count: 14 },
    { name: "Education & Skills", slug: "education-skills", icon: GraduationCap, count: 16 },
    { name: "Charity & Volunteering", slug: "charity-volunteering", icon: HandHeart, count: 21 },
  ];

  const featuredEvents = [
    {
      id: "demo-1",
      title: "Clean Rivers Community Clean-Up & Tree Drive",
      organization: "Green Earth Volunteers",
      category: "Environment",
      date: "Sat, Sep 12 • 09:00 AM",
      location: "Metropolis Riverside Park",
      capacity: 100,
      registered: 86,
      tags: ["Volunteering", "Eco", "Free"],
      status: "REGISTRATION_OPEN"
    },
    {
      id: "demo-2",
      title: "AI for Social Good & Civic Tech Hackathon",
      organization: "Metropolis Tech Council",
      category: "Tech",
      date: "Fri, Sep 18 • 10:00 AM",
      location: "Civic Innovation Hub / Hybrid",
      capacity: 150,
      registered: 150,
      waitlist: 24,
      tags: ["Hackathon", "AI", "Certificate"],
      status: "WAITLIST_AVAILABLE"
    },
    {
      id: "demo-3",
      title: "Youth Coding Bootcamp & Mentorship Circle",
      organization: "Education Forward Foundation",
      category: "Education",
      date: "Sun, Sep 20 • 02:00 PM",
      location: "Community Central Library",
      capacity: 40,
      registered: 32,
      tags: ["Workshop", "Beginner"],
      status: "REGISTRATION_OPEN"
    }
  ];

  return (
    <div className="flex flex-col min-h-screen">
      {/* 1. Hero Section */}
      <section className="relative overflow-hidden pt-12 pb-20 md:pt-20 md:pb-32 bg-gradient-to-b from-background via-emerald-950/10 to-background border-b border-border/40">
        <div className="container mx-auto px-4 sm:px-8 relative z-10 text-center max-w-4xl">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs font-semibold mb-6 animate-pulse">
            <Sparkles className="h-3.5 w-3.5" />
            <span>CivicPulse Platform v1.0 • Built for High-Concurreny Community Action</span>
          </div>

          {/* Heading */}
          <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-foreground leading-[1.15] mb-6">
            Engage, Organize, and Empower Your <span className="bg-clip-text text-transparent bg-gradient-to-r from-emerald-600 via-teal-500 to-emerald-400">Local Community</span>
          </h1>

          {/* Subheading */}
          <p className="text-base sm:text-lg text-muted-foreground max-w-2xl mx-auto mb-10 leading-relaxed">
            The all-in-one event platform engineered for colleges, NGOs, clubs, and civic groups. With concurrency-safe registrations, real-time waitlists, and instant QR check-ins.
          </p>

          {/* Search Bar */}
          <form onSubmit={handleSearchSubmit} className="max-w-2xl mx-auto mb-8">
            <div className="flex flex-col sm:flex-row items-center gap-2 p-2 rounded-2xl bg-card border border-border/80 shadow-xl shadow-emerald-950/5 focus-within:border-emerald-500/50 transition-all">
              <div className="flex items-center gap-2 flex-1 w-full px-3">
                <Search className="h-5 w-5 text-muted-foreground" />
                <Input
                  type="text"
                  placeholder="Search cleanups, hackathons, workshops, rallies..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="border-0 shadow-none focus-visible:ring-0 px-0 text-sm bg-transparent"
                />
              </div>
              <Button type="submit" variant="gradient" size="lg" className="w-full sm:w-auto font-medium gap-2">
                Discover
                <ArrowRight className="h-4 w-4" />
              </Button>
            </div>
          </form>

          {/* Quick Categories Bar */}
          <div className="flex flex-wrap items-center justify-center gap-2 text-xs text-muted-foreground">
            <span className="font-semibold text-foreground mr-1">Popular:</span>
            {categories.slice(0, 4).map((cat) => (
              <Link 
                key={cat.slug} 
                href={`/events?category=${cat.slug}`}
                className="px-2.5 py-1 rounded-lg bg-muted/60 hover:bg-muted text-muted-foreground hover:text-foreground border border-border/50 transition-colors"
              >
                {cat.name}
              </Link>
            ))}
          </div>
        </div>

        {/* Ambient background glows */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[300px] bg-emerald-500/10 blur-[120px] rounded-full pointer-events-none -z-0" />
      </section>

      {/* 2. Live Metrics Counter Bar */}
      <section className="border-b border-border/40 bg-muted/20 py-8">
        <div className="container mx-auto px-4 sm:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-center">
            <div className="space-y-1">
              <div className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">100%</div>
              <div className="text-xs text-muted-foreground font-medium">Zero Overselling Guarantee</div>
            </div>
            <div className="space-y-1">
              <div className="text-2xl sm:text-3xl font-extrabold text-emerald-600 dark:text-emerald-400 tracking-tight">&lt;50ms</div>
              <div className="text-xs text-muted-foreground font-medium">Pessimistic Lock Latency</div>
            </div>
            <div className="space-y-1">
              <div className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">Instant</div>
              <div className="text-xs text-muted-foreground font-medium">Signed QR Check-In</div>
            </div>
            <div className="space-y-1">
              <div className="text-2xl sm:text-3xl font-extrabold text-teal-600 dark:text-teal-400 tracking-tight">Real-Time</div>
              <div className="text-xs text-muted-foreground font-medium">Kafka Event Streaming</div>
            </div>
          </div>
        </div>
      </section>

      {/* 3. Featured Community Events Showcase */}
      <section className="py-16 md:py-24 container mx-auto px-4 sm:px-8">
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-12 gap-4">
          <div>
            <Badge variant="success" className="mb-2">Explore Active Drives</Badge>
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">Featured Community Events</h2>
            <p className="text-sm text-muted-foreground mt-1">Discover verified local drives and sign up with guaranteed capacity management.</p>
          </div>
          <Link href="/events">
            <Button variant="outline" size="sm" className="gap-1.5 self-start">
              View All Events
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {featuredEvents.map((event) => (
            <Card key={event.id} className="glow-card border-border/70 flex flex-col justify-between overflow-hidden group">
              <div className="p-6 space-y-4">
                <div className="flex items-center justify-between">
                  <Badge variant={event.status === "REGISTRATION_OPEN" ? "success" : "warning"}>
                    {event.status === "REGISTRATION_OPEN" ? "Registration Open" : "Waitlist Open"}
                  </Badge>
                  <span className="text-xs text-muted-foreground font-medium">{event.category}</span>
                </div>

                <div>
                  <h3 className="font-semibold text-lg text-foreground group-hover:text-primary transition-colors leading-snug">
                    {event.title}
                  </h3>
                  <p className="text-xs text-muted-foreground mt-1 font-medium">{event.organization}</p>
                </div>

                <div className="space-y-2 pt-2 text-xs text-muted-foreground border-t border-border/40">
                  <div className="flex items-center gap-2">
                    <Calendar className="h-3.5 w-3.5 text-primary" />
                    <span>{event.date}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <MapPin className="h-3.5 w-3.5 text-primary" />
                    <span className="truncate">{event.location}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Users className="h-3.5 w-3.5 text-primary" />
                    <span>
                      {event.registered}/{event.capacity} confirmed
                      {event.waitlist && ` • ${event.waitlist} on waitlist`}
                    </span>
                  </div>
                </div>

                <div className="flex flex-wrap gap-1.5 pt-1">
                  {event.tags.map((tag) => (
                    <span key={tag} className="text-[10px] px-2 py-0.5 rounded-md bg-muted text-muted-foreground font-medium">
                      #{tag}
                    </span>
                  ))}
                </div>
              </div>

              <div className="p-4 bg-muted/30 border-t border-border/40 flex items-center justify-between">
                <div className="w-2/3 bg-muted rounded-full h-1.5 overflow-hidden">
                  <div 
                    className="bg-emerald-500 h-full rounded-full" 
                    style={{ width: `${Math.min(100, (event.registered / event.capacity) * 100)}%` }}
                  />
                </div>
                <Link href={`/events/${event.id}`}>
                  <Button size="sm" variant="gradient" className="text-xs h-8">
                    View Event
                  </Button>
                </Link>
              </div>
            </Card>
          ))}
        </div>
      </section>

      {/* 4. Core Enterprise Capabilities */}
      <section className="py-16 md:py-24 bg-muted/30 border-y border-border/40">
        <div className="container mx-auto px-4 sm:px-8 max-w-5xl">
          <div className="text-center max-w-2xl mx-auto mb-16 space-y-3">
            <Badge variant="info">Engineered for Reliability</Badge>
            <h2 className="text-3xl font-bold tracking-tight">Why CivicPulse is Built Differently</h2>
            <p className="text-sm text-muted-foreground">
              Designed as a resilient SaaS platform with database-enforced integrity, real-time feedback, and strict authorization.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="p-6 rounded-2xl bg-card border border-border/80 space-y-3 shadow-sm">
              <div className="h-10 w-10 rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <h3 className="text-base font-semibold">Zero-Overselling Concurrency</h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                PostgreSQL row-level locking ensures that concurrent registration spikes never exceed venue capacity.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-card border border-border/80 space-y-3 shadow-sm">
              <div className="h-10 w-10 rounded-xl bg-teal-500/10 text-teal-600 dark:text-teal-400 flex items-center justify-center font-bold">
                <QrCode className="h-5 w-5" />
              </div>
              <h3 className="text-base font-semibold">HMAC-Signed QR Check-In</h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Dynamic, anti-fraud QR check-ins protect attendance records from replay attacks and duplicate entries.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-card border border-border/80 space-y-3 shadow-sm">
              <div className="h-10 w-10 rounded-xl bg-blue-500/10 text-blue-600 dark:text-blue-400 flex items-center justify-center font-bold">
                <TrendingUp className="h-5 w-5" />
              </div>
              <h3 className="text-base font-semibold">Real Database Analytics</h3>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Live attendance rate, no-show trends, and capacity utilization computed directly from SQL relational data.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 5. Call to Action for Organizers */}
      <section className="py-20 container mx-auto px-4 sm:px-8">
        <div className="rounded-3xl bg-gradient-to-tr from-emerald-900 via-teal-900 to-slate-900 text-white p-8 md:p-14 text-center max-w-4xl mx-auto shadow-2xl relative overflow-hidden">
          <div className="relative z-10 space-y-6">
            <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
              Ready to mobilize your community?
            </h2>
            <p className="text-sm sm:text-base text-emerald-100/80 max-w-xl mx-auto leading-relaxed">
              Create your organization in seconds, publish events, track volunteer attendance, and keep your community engaged.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
              <Link href="/register">
                <Button size="lg" className="bg-white text-emerald-900 hover:bg-emerald-50 font-semibold shadow-lg">
                  Create Organization Account
                </Button>
              </Link>
              <Link href="/events">
                <Button size="lg" variant="outline" className="text-white border-white/30 hover:bg-white/10">
                  Explore Public Calendar
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
