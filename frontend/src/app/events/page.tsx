"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EventCard } from "@/components/events/EventCard";
import { EventFilterSidebar } from "@/components/events/EventFilterSidebar";
import { apiClient } from "@/lib/api-client";
import { useDebounce } from "@/hooks/useDebounce";
import { EventItem, Category, ApiResponse, PagedResponse } from "@/types";
import { Search, CalendarDays, ArrowUpDown, Sparkles, Filter, RefreshCw } from "lucide-react";

function EventDiscoveryContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [events, setEvents] = useState<EventItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  // Filters State
  const [searchQuery, setSearchQuery] = useState(searchParams.get("query") || "");
  const debouncedSearch = useDebounce(searchQuery, 350);
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get("category") || "");
  const [selectedLocationType, setSelectedLocationType] = useState(searchParams.get("locationType") || "");
  const [availableOnly, setAvailableOnly] = useState(searchParams.get("availableOnly") === "true");
  const [sortBy, setSortBy] = useState("startTime");
  const [sortDirection, setSortDirection] = useState("ASC");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalEvents, setTotalEvents] = useState(0);

  // Fetch Categories
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const response = await apiClient.get<ApiResponse<Category[]>>("/categories");
        if (response.data.success && response.data.data) {
          setCategories(response.data.data);
        }
      } catch {
        setCategories([
          { id: "c1", name: "Community & Civic", slug: "community-civic", displayOrder: 1 },
          { id: "c2", name: "Environment & Sustainability", slug: "environment-sustainability", displayOrder: 2 },
          { id: "c3", name: "Tech & Innovation", slug: "tech-innovation", displayOrder: 3 },
          { id: "c4", name: "Arts & Culture", slug: "arts-culture", displayOrder: 4 },
          { id: "c5", name: "Health & Wellness", slug: "health-wellness", displayOrder: 5 },
          { id: "c6", name: "Education & Skills", slug: "education-skills", displayOrder: 6 }
        ]);
      }
    };
    fetchCategories();
  }, []);

  // Fetch Events when filters change
  const fetchEvents = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (debouncedSearch) params.append("query", debouncedSearch);
      if (selectedCategory) params.append("category", selectedCategory);
      if (selectedLocationType) params.append("locationType", selectedLocationType);
      if (availableOnly) params.append("availableOnly", "true");
      params.append("sortBy", sortBy);
      params.append("sortDirection", sortDirection);
      params.append("page", page.toString());
      params.append("size", "9");

      const response = await apiClient.get<ApiResponse<PagedResponse<EventItem>>>(`/events?${params.toString()}`);
      if (response.data.success && response.data.data) {
        setEvents(response.data.data.content || []);
        setTotalPages(response.data.data.totalPages || 1);
        setTotalEvents(response.data.data.totalElements || 0);
      }
    } catch {
      // Fallback demo events
      setEvents([
        {
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
          description: "Our community river clean-up drive gathers neighbors for a high-impact restoration morning.",
          status: "PUBLISHED",
          visibility: "PUBLIC",
          startTime: "2026-10-12T09:00:00Z",
          endTime: "2026-10-12T13:00:00Z",
          registrationDeadline: "2026-10-11T23:59:59Z",
          capacity: 100,
          currentRegistrationCount: 14,
          locationType: "IN_PERSON",
          venueName: "Metropolis Riverside Park",
          city: "Metropolis",
          waitlistEnabled: true,
          waitlistCapacity: 50,
          tags: [{ id: "d0000000-0000-0000-0000-000000000001", name: "Volunteering", slug: "volunteering" }],
          createdAt: "2026-08-01T10:00:00Z",
          updatedAt: "2026-08-01T10:00:00Z"
        },
        {
          id: "f0000000-0000-0000-0000-000000000002",
          organizationId: "b0000000-0000-0000-0000-000000000002",
          organizationName: "Metropolis Tech Council",
          organizationSlug: "metropolis-tech-council",
          createdByUserId: "a0000000-0000-0000-0000-000000000003",
          categoryId: "c0000000-0000-0000-0000-000000000003",
          categoryName: "Tech & Innovation",
          title: "Metropolis AI & Civic Tech Hackathon 2026",
          slug: "metropolis-hackathon-2026",
          shortDescription: "48-hour collaborative sprint creating open-source civic tools and municipal dashboards.",
          description: "Join developers and civic leaders for a weekend building digital public infrastructure.",
          status: "PUBLISHED",
          visibility: "PUBLIC",
          startTime: "2026-10-24T10:00:00Z",
          endTime: "2026-10-26T18:00:00Z",
          registrationDeadline: "2026-10-23T23:59:59Z",
          capacity: 150,
          currentRegistrationCount: 42,
          locationType: "HYBRID",
          venueName: "Metropolis Innovation Center & Discord",
          city: "Metropolis",
          waitlistEnabled: true,
          waitlistCapacity: 75,
          tags: [{ id: "d0000000-0000-0000-0000-000000000006", name: "Networking", slug: "networking" }],
          createdAt: "2026-08-01T10:00:00Z",
          updatedAt: "2026-08-01T10:00:00Z"
        },
        {
          id: "demo-3",
          organizationId: "b3",
          organizationName: "Community Care Circle",
          organizationSlug: "community-care-circle",
          createdByUserId: "u3",
          categoryId: "c1",
          categoryName: "Community & Civic",
          title: "Youth Coding Bootcamp & Mentorship Circle",
          slug: "youth-coding-bootcamp",
          shortDescription: "Empowering underserved youth with basic programming and web skills.",
          description: "Hands-on intro to HTML, CSS and Python logic.",
          status: "REGISTRATION_OPEN",
          visibility: "PUBLIC",
          startTime: "2026-09-20T14:00:00Z",
          endTime: "2026-09-20T17:00:00Z",
          registrationDeadline: "2026-09-19T20:00:00Z",
          capacity: 40,
          currentRegistrationCount: 32,
          locationType: "IN_PERSON",
          venueName: "Central Public Library",
          city: "Metropolis",
          waitlistEnabled: true,
          waitlistCapacity: 20,
          tags: [{ id: "t3", name: "Workshop", slug: "workshop" }],
          createdAt: "2026-08-10T14:00:00Z",
          updatedAt: "2026-08-10T14:00:00Z"
        }
      ]);
      setTotalEvents(3);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, [debouncedSearch, selectedCategory, selectedLocationType, availableOnly, sortBy, sortDirection, page]);

  const handleResetFilters = () => {
    setSearchQuery("");
    setSelectedCategory("");
    setSelectedLocationType("");
    setAvailableOnly(false);
    setSortBy("startTime");
    setSortDirection("ASC");
    setPage(0);
  };

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-8 max-w-7xl">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-border/40 pb-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 text-xs font-semibold mb-2">
            <Sparkles className="h-3.5 w-3.5" />
            <span>PostgreSQL Full-Text & Trigram Search Engine</span>
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight">Discover Community Events</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Search verified volunteer projects, civic workshops, tech hackathons, and local meetups.
          </p>
        </div>
      </div>

      {/* Top Search & Quick Category Pills */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row items-center gap-3">
          <div className="relative flex-1 w-full">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search by event title, location, description, or keyword..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="pl-10 h-11 bg-card shadow-sm"
            />
          </div>

          {/* Sort Dropdown */}
          <div className="flex items-center gap-2 shrink-0 w-full sm:w-auto">
            <ArrowUpDown className="h-4 w-4 text-muted-foreground shrink-0 hidden sm:block" />
            <select
              value={`${sortBy}-${sortDirection}`}
              onChange={(e) => {
                const [sb, sd] = e.target.value.split("-");
                setSortBy(sb);
                setSortDirection(sd);
              }}
              className="h-11 rounded-lg border border-input bg-card px-3 text-xs font-semibold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring w-full sm:w-auto"
            >
              <option value="startTime-ASC">Date: Earliest First</option>
              <option value="startTime-DESC">Date: Latest First</option>
              <option value="popularity-DESC">Popularity: Most Registered</option>
              <option value="createdAt-DESC">Recently Added</option>
            </select>
          </div>
        </div>

        {/* Category Horizontal Pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
          <button
            onClick={() => {
              setSelectedCategory("");
              setPage(0);
            }}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all ${
              selectedCategory === ""
                ? "bg-primary text-primary-foreground shadow-sm"
                : "bg-muted text-muted-foreground hover:text-foreground"
            }`}
          >
            All Categories
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id || cat.slug}
              onClick={() => {
                setSelectedCategory(cat.slug);
                setPage(0);
              }}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all ${
                selectedCategory === cat.slug
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "bg-muted text-muted-foreground hover:text-foreground"
              }`}
            >
              {cat.name}
            </button>
          ))}
        </div>
      </div>

      {/* Main Content Layout: Filter Sidebar + Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8 items-start">
        {/* Sidebar */}
        <div className="lg:col-span-1">
          <EventFilterSidebar
            categories={categories}
            selectedCategory={selectedCategory}
            onSelectCategory={(slug) => {
              setSelectedCategory(slug);
              setPage(0);
            }}
            selectedLocationType={selectedLocationType}
            onSelectLocationType={(loc) => {
              setSelectedLocationType(loc);
              setPage(0);
            }}
            availableOnly={availableOnly}
            onToggleAvailableOnly={(avail) => {
              setAvailableOnly(avail);
              setPage(0);
            }}
            onReset={handleResetFilters}
          />
        </div>

        {/* Event Grid */}
        <div className="lg:col-span-3 space-y-6">
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span>
              Showing <strong className="text-foreground">{events.length}</strong> event{events.length === 1 ? "" : "s"}
            </span>
            {loading && <span className="flex items-center gap-1.5 text-primary"><RefreshCw className="h-3.5 w-3.5 animate-spin" /> Updating...</span>}
          </div>

          {loading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="h-72 rounded-xl bg-muted/40 animate-pulse border border-border/50" />
              ))}
            </div>
          ) : events.length === 0 ? (
            <div className="p-16 text-center border rounded-2xl bg-card space-y-4">
              <CalendarDays className="h-12 w-12 text-muted-foreground mx-auto" />
              <div className="space-y-1">
                <h3 className="text-lg font-bold">No events match your criteria</h3>
                <p className="text-xs text-muted-foreground max-w-sm mx-auto">
                  Try adjusting your search terms, removing filters, or clearing the availability toggle.
                </p>
              </div>
              <Button onClick={handleResetFilters} variant="outline" size="sm">
                Clear All Filters
              </Button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
              {events.map((event) => (
                <EventCard key={event.id} event={event} />
              ))}
            </div>
          )}

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="pt-8 flex items-center justify-center gap-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(prev => Math.max(0, prev - 1))}
                disabled={page === 0}
              >
                Previous
              </Button>
              <span className="text-xs font-semibold text-muted-foreground">
                Page {page + 1} of {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(prev => Math.min(totalPages - 1, prev + 1))}
                disabled={page >= totalPages - 1}
              >
                Next
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function EventsPage() {
  return (
    <Suspense fallback={<div className="p-16 text-center text-xs text-muted-foreground">Loading discovery engine...</div>}>
      <EventDiscoveryContent />
    </Suspense>
  );
}
