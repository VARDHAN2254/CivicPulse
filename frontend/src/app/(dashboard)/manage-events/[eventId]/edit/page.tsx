"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { apiClient } from "@/lib/api-client";
import { Category, ApiResponse, EventItem } from "@/types";
import { 
  CalendarDays, 
  MapPin, 
  Globe, 
  Users, 
  ArrowLeft, 
  AlertCircle, 
  Save, 
  Clock,
  Layers
} from "lucide-react";

export default function EditEventPage() {
  const params = useParams();
  const router = useRouter();
  const eventId = params.eventId as string;

  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  // Form Fields
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
  const [tagNames, setTagNames] = useState("");

  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [catsRes, eventRes] = await Promise.all([
          apiClient.get<ApiResponse<Category[]>>("/categories"),
          apiClient.get<ApiResponse<EventItem>>(`/events/${eventId}`),
        ]);

        if (catsRes.data.success && catsRes.data.data) {
          setCategories(catsRes.data.data);
        }

        if (eventRes.data.success && eventRes.data.data) {
          const ev = eventRes.data.data;
          setCategoryId(ev.categoryId);
          setTitle(ev.title);
          setShortDescription(ev.shortDescription || "");
          setDescription(ev.description);
          setStartTime(ev.startTime ? new Date(ev.startTime).toISOString().slice(0, 16) : "");
          setEndTime(ev.endTime ? new Date(ev.endTime).toISOString().slice(0, 16) : "");
          setRegistrationDeadline(ev.registrationDeadline ? new Date(ev.registrationDeadline).toISOString().slice(0, 16) : "");
          setCapacity(ev.capacity);
          setWaitlistEnabled(ev.waitlistEnabled);
          setWaitlistCapacity(ev.waitlistCapacity);
          setLocationType(ev.locationType);
          setVenueName(ev.venueName || "");
          setAddress(ev.address || "");
          setCity(ev.city || "");
          setState(ev.state || "");
          setPostalCode(ev.postalCode || "");
          setVirtualMeetingUrl(ev.virtualMeetingUrl || "");
          setBannerImageUrl(ev.bannerImageUrl || "");
          setTagNames(ev.tags ? ev.tags.map(t => t.name).join(", ") : "");
        }
      } catch {
        // Fallback for preview
        setCategories([
          { id: "c0000000-0000-0000-0000-000000000001", name: "Community & Civic", slug: "community-civic", displayOrder: 1 },
          { id: "c0000000-0000-0000-0000-000000000002", name: "Environment & Sustainability", slug: "environment-sustainability", displayOrder: 2 }
        ]);
        setCategoryId("c0000000-0000-0000-0000-000000000001");
        setTitle("Clean Rivers Community Clean-Up & Tree Drive");
        setDescription("Grassroots river cleaning drive at the metropolis riverside.");
        setStartTime("2026-09-12T09:00");
        setEndTime("2026-09-12T13:00");
        setRegistrationDeadline("2026-09-11T18:00");
      } finally {
        setLoadingInitial(false);
      }
    };
    fetchData();
  }, [eventId]);

  const [loadingInitial, setLoadingInitial] = useState(true);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const parsedTags = tagNames.split(",").map(t => t.trim()).filter(Boolean);

      const payload = {
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

      const response = await apiClient.put<ApiResponse<EventItem>>(`/events/${eventId}`, payload);
      if (response.data.success) {
        router.push("/manage-events");
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to update event.");
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
        <h1 className="text-3xl font-extrabold tracking-tight">Edit Event Details</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Modify event schedule, location, capacity, or event description.
        </p>
      </div>

      <form onSubmit={handleSubmit}>
        <Card className="border-border">
          <CardHeader>
            <CardTitle className="text-lg font-bold">Event Configuration</CardTitle>
            <CardDescription className="text-xs">
              Saved changes will be immediately visible on public event pages.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-6">
            {error && (
              <div className="flex items-center gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-xs">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Category */}
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

            {/* Title */}
            <div className="space-y-2">
              <label className="text-xs font-semibold">Event Title</label>
              <Input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </div>

            {/* Short Description */}
            <div className="space-y-2">
              <label className="text-xs font-semibold">Short Summary</label>
              <Input
                type="text"
                value={shortDescription}
                onChange={(e) => setShortDescription(e.target.value)}
              />
            </div>

            {/* Description */}
            <div className="space-y-2">
              <label className="text-xs font-semibold">Full Event Description</label>
              <textarea
                rows={5}
                required
                className="flex w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            {/* Dates */}
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

            {/* Capacity */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2 border-t">
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
                <label className="text-xs font-semibold">Tags (comma separated)</label>
                <Input
                  type="text"
                  value={tagNames}
                  onChange={(e) => setTagNames(e.target.value)}
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
              {submitting ? "Saving Changes..." : "Save Changes"}
            </Button>
          </CardFooter>
        </Card>
      </form>
    </div>
  );
}
