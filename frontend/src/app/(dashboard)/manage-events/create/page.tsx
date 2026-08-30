"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { apiClient } from "@/lib/api-client";
import { Organization, Category, ApiResponse, EventItem } from "@/types";
import { 
  CalendarDays, 
  MapPin, 
  Globe, 
  Users, 
  ArrowLeft, 
  AlertCircle, 
  Save, 
  Sparkles, 
  Clock,
  Layers,
  Building2
} from "lucide-react";

export default function CreateEventPage() {
  const router = useRouter();

  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loadingInitial, setLoadingInitial] = useState(true);

  // Form Fields
  const [organizationId, setOrganizationId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [title, setTitle] = useState("");
  const [shortDescription, setShortDescription] = useState("");
  const [description, setDescription] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [registrationDeadline, setRegistrationDeadline] = useState("");
  const [capacity, setCapacity] = useState(50);
  const [waitlistEnabled, setWaitlistEnabled] = useState(true);
  const [waitlistCapacity, setWaitlistCapacity] = useState(50);
  const [locationType, setLocationType] = useState<"IN_PERSON" | "VIRTUAL" | "HYBRID">("IN_PERSON");
  const [venueName, setVenueName] = useState("");
  const [address, setAddress] = useState("");
  const [city, setCity] = useState("");
  const [state, setState] = useState("");
  const [postalCode, setPostalCode] = useState("");
  const [virtualMeetingUrl, setVirtualMeetingUrl] = useState("");
  const [bannerImageUrl, setBannerImageUrl] = useState("");
  const [tagNames, setTagNames] = useState("Volunteering, Community, Free");

  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [orgsRes, catsRes] = await Promise.all([
          apiClient.get<ApiResponse<Organization[]>>("/organizations/my"),
          apiClient.get<ApiResponse<Category[]>>("/categories"),
        ]);

        if (orgsRes.data.success && orgsRes.data.data && orgsRes.data.data.length > 0) {
          setOrganizations(orgsRes.data.data);
          setOrganizationId(orgsRes.data.data[0].id);
        }
        if (catsRes.data.success && catsRes.data.data && catsRes.data.data.length > 0) {
          setCategories(catsRes.data.data);
          setCategoryId(catsRes.data.data[0].id);
        }
      } catch {
        // Fallback for dev preview
        setOrganizations([
          {
            id: "b0000000-0000-0000-0000-000000000001",
            name: "Green Earth Volunteers",
            slug: "green-earth-volunteers",
            isVerified: true,
            createdAt: "2026-01-15T08:00:00Z"
          }
        ]);
        setOrganizationId("b0000000-0000-0000-0000-000000000001");
        setCategories([
          { id: "c0000000-0000-0000-0000-000000000001", name: "Community & Civic", slug: "community-civic", displayOrder: 1 },
          { id: "c0000000-0000-0000-0000-000000000002", name: "Environment & Sustainability", slug: "environment-sustainability", displayOrder: 2 },
          { id: "c0000000-0000-0000-0000-000000000003", name: "Tech & Innovation", slug: "tech-innovation", displayOrder: 3 }
        ]);
        setCategoryId("c0000000-0000-0000-0000-000000000001");
      } finally {
        setLoadingInitial(false);
      }
    };
    fetchData();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!startTime || !endTime || !registrationDeadline) {
      setError("Please set start time, end time, and registration deadline.");
      return;
    }

    if (new Date(startTime) >= new Date(endTime)) {
      setError("Event start time must be before end time.");
      return;
    }

    if (new Date(registrationDeadline) > new Date(startTime)) {
      setError("Registration deadline cannot be after event start time.");
      return;
    }

    setSubmitting(true);
    try {
      const parsedTags = tagNames.split(",").map(t => t.trim()).filter(Boolean);

      const payload = {
        organizationId,
        categoryId,
        title: title.trim(),
        shortDescription: shortDescription.trim(),
        description: description.trim(),
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        registrationDeadline: new Date(registrationDeadline).toISOString(),
        capacity: Number(capacity),
        waitlistEnabled,
        waitlistCapacity: Number(waitlistCapacity),
        locationType,
        venueName: venueName.trim() || undefined,
        address: address.trim() || undefined,
        city: city.trim() || undefined,
        state: state.trim() || undefined,
        postalCode: postalCode.trim() || undefined,
        virtualMeetingUrl: virtualMeetingUrl.trim() || undefined,
        bannerImageUrl: bannerImageUrl.trim() || undefined,
        tagNames: parsedTags,
      };

      const response = await apiClient.post<ApiResponse<EventItem>>("/events", payload);
      if (response.data.success && response.data.data) {
        router.push("/manage-events");
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to create event. Please check inputs.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-8 max-w-4xl">
      <Link href="/manage-events" className="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground">
        <ArrowLeft className="h-3.5 w-3.5" />
        Back to Event Studio
      </Link>

      <div>
        <h1 className="text-3xl font-extrabold tracking-tight">Create Community Event</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Configure event details, registration capacity, location, and waitlist rules.
        </p>
      </div>

      <form onSubmit={handleSubmit}>
        <Card className="border-border">
          <CardHeader>
            <CardTitle className="text-lg font-bold">Event Details & Schedule</CardTitle>
            <CardDescription className="text-xs">
              Provide complete information to help community members discover and register for your drive.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-6">
            {error && (
              <div className="flex items-center gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-xs">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Organization & Category Selectors */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-xs font-semibold flex items-center gap-1.5">
                  <Building2 className="h-3.5 w-3.5 text-primary" />
                  Hosting Organization
                </label>
                <select
                  value={organizationId}
                  onChange={(e) => setOrganizationId(e.target.value)}
                  required
                  className="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {organizations.map((org) => (
                    <option key={org.id} value={org.id}>
                      {org.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold flex items-center gap-1.5">
                  <Layers className="h-3.5 w-3.5 text-primary" />
                  Event Category
                </label>
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value)}
                  required
                  className="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Event Title */}
            <div className="space-y-2">
              <label className="text-xs font-semibold">Event Title</label>
              <Input
                type="text"
                required
                placeholder="e.g. 2026 Metropolis River Cleanup & Tree Drive"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </div>

            {/* Short & Full Description */}
            <div className="space-y-2">
              <label className="text-xs font-semibold">Short Summary (1-2 sentences)</label>
              <Input
                type="text"
                placeholder="Brief summary for event cards and search snippets"
                value={shortDescription}
                onChange={(e) => setShortDescription(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold">Full Event Description</label>
              <textarea
                rows={5}
                required
                placeholder="Describe schedule, what attendees should bring, assembly point, volunteer requirements..."
                className="flex w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            {/* Schedule Dates */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2 border-t">
              <div className="space-y-2">
                <label className="text-xs font-semibold flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-primary" />
                  Start Date & Time
                </label>
                <Input
                  type="datetime-local"
                  required
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-primary" />
                  End Date & Time
                </label>
                <Input
                  type="datetime-local"
                  required
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-primary" />
                  Registration Deadline
                </label>
                <Input
                  type="datetime-local"
                  required
                  value={registrationDeadline}
                  onChange={(e) => setRegistrationDeadline(e.target.value)}
                />
              </div>
            </div>

            {/* Capacity & Waitlist */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2 border-t">
              <div className="space-y-2">
                <label className="text-xs font-semibold flex items-center gap-1.5">
                  <Users className="h-3.5 w-3.5 text-primary" />
                  Total Attendee Capacity
                </label>
                <Input
                  type="number"
                  min={1}
                  required
                  value={capacity}
                  onChange={(e) => setCapacity(Number(e.target.value))}
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold">Enable Automated Waitlist</label>
                <div className="flex items-center h-10">
                  <input
                    type="checkbox"
                    id="waitlistCheckbox"
                    checked={waitlistEnabled}
                    onChange={(e) => setWaitlistEnabled(e.target.checked)}
                    className="h-4 w-4 rounded border-gray-300 text-emerald-600 focus:ring-emerald-500"
                  />
                  <label htmlFor="waitlistCheckbox" className="ml-2 text-xs text-muted-foreground font-medium">
                    Auto-promote waitlist on cancellation
                  </label>
                </div>
              </div>

              {waitlistEnabled && (
                <div className="space-y-2">
                  <label className="text-xs font-semibold">Waitlist Capacity</label>
                  <Input
                    type="number"
                    min={1}
                    value={waitlistCapacity}
                    onChange={(e) => setWaitlistCapacity(Number(e.target.value))}
                  />
                </div>
              )}
            </div>

            {/* Location Type */}
            <div className="space-y-3 pt-2 border-t">
              <label className="text-xs font-semibold">Location Type</label>
              <div className="grid grid-cols-3 gap-3">
                {(["IN_PERSON", "VIRTUAL", "HYBRID"] as const).map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setLocationType(type)}
                    className={`flex items-center justify-center gap-2 p-2.5 rounded-lg border text-xs font-medium transition-all ${
                      locationType === type
                        ? "border-emerald-600 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-semibold"
                        : "border-border bg-card hover:bg-muted text-muted-foreground"
                    }`}
                  >
                    {type === "VIRTUAL" ? <Globe className="h-4 w-4" /> : <MapPin className="h-4 w-4" />}
                    {type.replace("_", " ")}
                  </button>
                ))}
              </div>

              {locationType !== "VIRTUAL" && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
                  <div className="space-y-2 sm:col-span-2">
                    <label className="text-xs font-semibold">Venue Name</label>
                    <Input
                      type="text"
                      placeholder="e.g. Riverside Park Pavilion #3"
                      value={venueName}
                      onChange={(e) => setVenueName(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2 sm:col-span-2">
                    <label className="text-xs font-semibold">Street Address</label>
                    <Input
                      type="text"
                      placeholder="e.g. 100 Waterfront Drive"
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-semibold">City</label>
                    <Input
                      type="text"
                      placeholder="Metropolis"
                      value={city}
                      onChange={(e) => setCity(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-semibold">State / Province</label>
                    <Input
                      type="text"
                      placeholder="NY"
                      value={state}
                      onChange={(e) => setState(e.target.value)}
                    />
                  </div>
                </div>
              )}

              {locationType !== "IN_PERSON" && (
                <div className="space-y-2 pt-2">
                  <label className="text-xs font-semibold">Virtual Meeting / Webinar URL</label>
                  <Input
                    type="url"
                    placeholder="https://meet.google.com/abc-defg-hij"
                    value={virtualMeetingUrl}
                    onChange={(e) => setVirtualMeetingUrl(e.target.value)}
                  />
                </div>
              )}
            </div>

            {/* Tags & Banner */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2 border-t">
              <div className="space-y-2">
                <label className="text-xs font-semibold">Tags (comma separated)</label>
                <Input
                  type="text"
                  placeholder="Volunteering, TreeDrive, Eco"
                  value={tagNames}
                  onChange={(e) => setTagNames(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold">Banner Image URL (Optional)</label>
                <Input
                  type="url"
                  placeholder="https://images.unsplash.com/photo-..."
                  value={bannerImageUrl}
                  onChange={(e) => setBannerImageUrl(e.target.value)}
                />
              </div>
            </div>
          </CardContent>

          <CardFooter className="border-t flex justify-end gap-3 pt-4">
            <Link href="/manage-events">
              <Button type="button" variant="ghost" size="sm">
                Cancel
              </Button>
            </Link>
            <Button type="submit" variant="gradient" size="sm" className="gap-2 font-medium" disabled={submitting}>
              <Save className="h-4 w-4" />
              {submitting ? "Saving Draft..." : "Create Event Draft"}
            </Button>
          </CardFooter>
        </Card>
      </form>
    </div>
  );
}
