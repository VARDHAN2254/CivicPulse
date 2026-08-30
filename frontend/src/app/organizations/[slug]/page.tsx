"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { Organization, ApiResponse } from "@/types";
import { 
  Building2, 
  CheckCircle2, 
  Globe, 
  Mail, 
  CalendarDays, 
  Users, 
  ArrowLeft, 
  Sparkles,
  MapPin,
  Calendar
} from "lucide-react";

export default function OrganizationDetailPage() {
  const params = useParams();
  const slug = params.slug as string;
  const [org, setOrg] = useState<Organization | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrg = async () => {
      try {
        const response = await apiClient.get<ApiResponse<Organization>>(`/organizations/${slug}`);
        if (response.data.success && response.data.data) {
          setOrg(response.data.data);
        }
      } catch {
        // Fallback demo org
        setOrg({
          id: "b0000000-0000-0000-0000-000000000001",
          name: "Green Earth Volunteers",
          slug: "green-earth-volunteers",
          description: "Grassroots environmental protection, urban afforestation, and river rejuvenation drives across the metro region. Organizing weekly volunteer tree planting and waste cleanup programs.",
          website: "https://greenearth.org",
          contactEmail: "contact@greenearth.org",
          isVerified: true,
          createdAt: "2026-01-15T08:00:00Z"
        });
      } finally {
        setLoading(false);
      }
    };
    fetchOrg();
  }, [slug]);

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-16 text-center">
        <div className="h-8 w-48 bg-muted animate-pulse mx-auto rounded-lg mb-4" />
        <div className="h-4 w-96 bg-muted animate-pulse mx-auto rounded-lg" />
      </div>
    );
  }

  if (!org) {
    return (
      <div className="container mx-auto px-4 py-16 text-center space-y-4">
        <h2 className="text-2xl font-bold">Organization Not Found</h2>
        <Link href="/organizations">
          <Button variant="outline" className="gap-2">
            <ArrowLeft className="h-4 w-4" />
            Back to Organizations
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-10 max-w-5xl">
      <Link href="/organizations" className="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground">
        <ArrowLeft className="h-3.5 w-3.5" />
        Back to all organizations
      </Link>

      {/* Header Banner */}
      <div className="p-8 rounded-3xl bg-gradient-to-tr from-card via-background to-emerald-950/20 border border-border shadow-lg flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="flex items-center gap-5">
          <div className="h-20 w-20 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white flex items-center justify-center font-extrabold text-3xl shadow-md shrink-0">
            {org.name.charAt(0).toUpperCase()}
          </div>
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-foreground">{org.name}</h1>
              {org.isVerified && (
                <Badge variant="success" className="gap-1 text-xs">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  Verified
                </Badge>
              )}
            </div>
            <p className="text-xs text-muted-foreground font-mono">@{org.slug}</p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {org.website && (
            <a href={org.website} target="_blank" rel="noreferrer">
              <Button variant="outline" size="sm" className="gap-1.5 text-xs">
                <Globe className="h-3.5 w-3.5 text-primary" />
                Visit Website
              </Button>
            </a>
          )}
          {org.contactEmail && (
            <a href={`mailto:${org.contactEmail}`}>
              <Button variant="secondary" size="sm" className="gap-1.5 text-xs">
                <Mail className="h-3.5 w-3.5 text-primary" />
                Contact
              </Button>
            </a>
          )}
        </div>
      </div>

      {/* Main Details & Events Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Bio & Details */}
        <div className="md:col-span-1 space-y-6">
          <Card className="border-border">
            <CardHeader>
              <CardTitle className="text-base font-bold">About Organization</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-xs text-muted-foreground leading-relaxed">
              <p>{org.description}</p>

              <div className="pt-3 border-t space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground font-medium">Chapter Status</span>
                  <span className="text-foreground font-semibold">Active & Certified</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground font-medium">Community Drives</span>
                  <span className="text-foreground font-semibold">12 Events Hosted</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Hosted Events */}
        <div className="md:col-span-2 space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold tracking-tight flex items-center gap-2">
              <CalendarDays className="h-5 w-5 text-primary" />
              Community Events & Drives
            </h2>
          </div>

          <div className="space-y-4">
            <Card className="glow-card border-border/80 p-5 space-y-3">
              <div className="flex items-center justify-between">
                <Badge variant="success">Registration Open</Badge>
                <span className="text-xs text-muted-foreground">Environment</span>
              </div>
              <h3 className="font-bold text-base hover:text-primary transition-colors">
                Clean Rivers Community Clean-Up & Tree Drive
              </h3>
              <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <Calendar className="h-3.5 w-3.5 text-primary" />
                  Sat, Sep 12 • 09:00 AM
                </span>
                <span className="flex items-center gap-1.5">
                  <MapPin className="h-3.5 w-3.5 text-primary" />
                  Metropolis Riverside Park
                </span>
              </div>
              <div className="pt-2 flex justify-between items-center border-t">
                <span className="text-xs text-muted-foreground">86/100 spots booked</span>
                <Link href="/events/demo-1">
                  <Button size="sm" variant="gradient" className="text-xs h-8">
                    View & Register
                  </Button>
                </Link>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
