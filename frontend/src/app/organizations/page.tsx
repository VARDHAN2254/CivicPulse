"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { OrgCard } from "@/components/organizations/OrgCard";
import { apiClient } from "@/lib/api-client";
import { Organization, ApiResponse, PagedResponse } from "@/types";
import { Building2, Search, PlusCircle, Sparkles, Filter } from "lucide-react";

export default function OrganizationsPage() {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(true);

  const fetchOrganizations = async (query: string = "") => {
    setLoading(true);
    try {
      const response = await apiClient.get<ApiResponse<PagedResponse<Organization>>>(
        `/organizations${query ? `?query=${encodeURIComponent(query)}` : ""}`
      );
      if (response.data.success && response.data.data) {
        setOrganizations(response.data.data.content || []);
      }
    } catch {
      // Fallback demo data if backend is offline in preview
      setOrganizations([
        {
          id: "b0000000-0000-0000-0000-000000000001",
          name: "Green Earth Volunteers",
          slug: "green-earth-volunteers",
          description: "Grassroots environmental protection, urban afforestation, and river rejuvenation drives across the metro region.",
          website: "https://greenearth.org",
          contactEmail: "contact@greenearth.org",
          isVerified: true,
          createdAt: "2026-01-15T08:00:00Z"
        },
        {
          id: "b0000000-0000-0000-0000-000000000002",
          name: "Metropolis Tech Council",
          slug: "metropolis-tech-council",
          description: "Fostering civic technology, AI for good, open government data initiatives, and inclusive coding bootcamps.",
          website: "https://metropolistech.org",
          contactEmail: "info@metropolistech.org",
          isVerified: true,
          createdAt: "2026-02-01T10:00:00Z"
        },
        {
          id: "b0000000-0000-0000-0000-000000000003",
          name: "Community Care Circle",
          slug: "community-care-circle",
          description: "Providing neighborhood pantry distribution, senior care visits, and mental wellness support circles.",
          website: "https://communitycare.org",
          contactEmail: "hello@communitycare.org",
          isVerified: false,
          createdAt: "2026-03-10T12:00:00Z"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrganizations();
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchOrganizations(searchQuery);
  };

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-8">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-border/40 pb-8">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 text-xs font-semibold mb-2">
            <Building2 className="h-3.5 w-3.5" />
            <span>Civic Chapters & Community Hubs</span>
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight">Community Organizations</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Discover verified NGOs, college clubs, and local action committees driving community projects.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link href="/manage-organizations">
            <Button variant="gradient" size="sm" className="gap-2 font-medium">
              <PlusCircle className="h-4 w-4" />
              Manage Organizations
            </Button>
          </Link>
        </div>
      </div>

      {/* Search & Filter Bar */}
      <form onSubmit={handleSearch} className="flex flex-col sm:flex-row items-center gap-3">
        <div className="relative flex-1 w-full">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Search by organization name or focus area..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9 bg-card"
          />
        </div>
        <Button type="submit" variant="secondary" size="default" className="w-full sm:w-auto gap-2">
          Search
        </Button>
      </form>

      {/* Grid of Organizations */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-48 rounded-xl border bg-muted/40 animate-pulse" />
          ))}
        </div>
      ) : organizations.length === 0 ? (
        <div className="p-12 text-center border rounded-2xl bg-card space-y-3">
          <Building2 className="h-10 w-10 text-muted-foreground mx-auto" />
          <h3 className="text-base font-semibold">No organizations found</h3>
          <p className="text-xs text-muted-foreground">Try broadening your search query or create a new organization.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {organizations.map((org) => (
            <OrgCard key={org.id} org={org} />
          ))}
        </div>
      )}
    </div>
  );
}
