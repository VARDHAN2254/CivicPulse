import React from "react";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EventItem } from "@/types";
import { formatDate } from "@/lib/utils";
import { Calendar, MapPin, Globe, Users, Building2, ArrowRight } from "lucide-react";

export function EventCard({ event }: { event: EventItem }) {
  const isFull = event.currentRegistrationCount >= event.capacity;

  return (
    <Card className="glow-card border-border/80 flex flex-col justify-between overflow-hidden group">
      <CardContent className="p-6 space-y-4">
        {/* Status and Category Badge */}
        <div className="flex items-center justify-between">
          <Badge variant={
            event.status === "REGISTRATION_OPEN" ? "success" :
            event.status === "PUBLISHED" ? "info" :
            isFull ? "warning" : "secondary"
          }>
            {isFull ? (event.waitlistEnabled ? "Waitlist Open" : "Event Full") : "Spots Available"}
          </Badge>
          <span className="text-xs text-muted-foreground font-medium">{event.categoryName}</span>
        </div>

        {/* Title & Organization */}
        <div>
          <h3 className="font-bold text-base text-foreground group-hover:text-primary transition-colors leading-snug">
            <Link href={`/events/${event.slug || event.id}`}>{event.title}</Link>
          </h3>
          {event.organizationName && (
            <span className="text-xs text-muted-foreground mt-1 flex items-center gap-1 font-medium">
              <Building2 className="h-3 w-3" />
              {event.organizationName}
            </span>
          )}
        </div>

        {event.shortDescription && (
          <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
            {event.shortDescription}
          </p>
        )}

        {/* Metadata info */}
        <div className="space-y-2 pt-2 text-xs text-muted-foreground border-t border-border/40">
          <div className="flex items-center gap-2">
            <Calendar className="h-3.5 w-3.5 text-primary shrink-0" />
            <span className="truncate">{formatDate(event.startTime)}</span>
          </div>

          <div className="flex items-center gap-2">
            {event.locationType === "VIRTUAL" ? (
              <Globe className="h-3.5 w-3.5 text-primary shrink-0" />
            ) : (
              <MapPin className="h-3.5 w-3.5 text-primary shrink-0" />
            )}
            <span className="truncate">{event.venueName || event.city || "Online Meeting"}</span>
          </div>

          <div className="flex items-center gap-2">
            <Users className="h-3.5 w-3.5 text-primary shrink-0" />
            <span>{event.currentRegistrationCount} / {event.capacity} registered</span>
          </div>
        </div>

        {/* Tag pills */}
        {event.tags && event.tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5 pt-1">
            {event.tags.slice(0, 3).map((tag) => (
              <span key={tag.id || tag.slug} className="text-[10px] px-2 py-0.5 rounded-md bg-muted text-muted-foreground font-medium">
                #{tag.name}
              </span>
            ))}
          </div>
        )}
      </CardContent>

      {/* Footer CTA & Progress */}
      <div className="p-4 bg-muted/30 border-t border-border/40 flex items-center justify-between gap-3">
        <div className="w-1/2 bg-muted rounded-full h-1.5 overflow-hidden">
          <div 
            className="bg-emerald-500 h-full rounded-full transition-all" 
            style={{ width: `${Math.min(100, (event.currentRegistrationCount / event.capacity) * 100)}%` }}
          />
        </div>
        <Link href={`/events/${event.slug || event.id}`}>
          <Button size="sm" variant="gradient" className="text-xs h-8 gap-1">
            Details
            <ArrowRight className="h-3 w-3" />
          </Button>
        </Link>
      </div>
    </Card>
  );
}
