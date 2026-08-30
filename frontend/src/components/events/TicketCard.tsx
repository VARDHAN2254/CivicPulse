"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { RegistrationDto } from "@/types";
import { formatDate } from "@/lib/utils";
import { DynamicQrModal } from "@/components/events/DynamicQrModal";
import { Ticket, Calendar, MapPin, Building2, QrCode, XCircle, ArrowRight, Scan } from "lucide-react";

interface TicketCardProps {
  ticket: RegistrationDto;
  onCancel?: (registrationId: string) => void;
}

export function TicketCard({ ticket, onCancel }: TicketCardProps) {
  const [cancelling, setCancelling] = useState(false);
  const [showQrModal, setShowQrModal] = useState(false);

  const handleCancelClick = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm("Are you sure you want to cancel this ticket pass? If there is a waitlist, your spot will be immediately offered to the next candidate.")) return;
    if (onCancel) {
      setCancelling(true);
      try {
        await onCancel(ticket.id);
      } finally {
        setCancelling(false);
      }
    }
  };

  return (
    <>
      <Card className="glow-card border-border/80 flex flex-col justify-between overflow-hidden bg-card">
        <CardContent className="p-6 space-y-4">
          {/* Header Ribbon */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="p-1.5 rounded-lg bg-primary/10 text-primary">
                <Ticket className="h-4 w-4" />
              </div>
              <span className="font-mono text-xs font-bold text-primary tracking-wider">{ticket.ticketCode}</span>
            </div>

            <Badge variant={ticket.status === "CONFIRMED" ? "success" : "secondary"}>
              {ticket.status}
            </Badge>
          </div>

          {/* Title & Organization */}
          <div>
            <h3 className="font-bold text-base text-foreground leading-snug">
              <Link href={`/events/${ticket.eventSlug || ticket.eventId}`} className="hover:text-primary transition-colors">
                {ticket.eventTitle}
              </Link>
            </h3>
            {ticket.organizationName && (
              <span className="text-xs text-muted-foreground mt-1 flex items-center gap-1">
                <Building2 className="h-3 w-3" />
                {ticket.organizationName}
              </span>
            )}
          </div>

          {/* Schedule & Location */}
          <div className="space-y-1.5 pt-2 text-xs text-muted-foreground border-t border-border/40">
            <div className="flex items-center gap-2">
              <Calendar className="h-3.5 w-3.5 text-primary shrink-0" />
              <span>{formatDate(ticket.startTime)}</span>
            </div>
            <div className="flex items-center gap-2">
              <MapPin className="h-3.5 w-3.5 text-primary shrink-0" />
              <span className="truncate">{ticket.venueName || "Metropolis"}</span>
            </div>
          </div>

          {/* QR Code Trigger Section */}
          <div 
            onClick={(e) => {
              e.stopPropagation();
              setShowQrModal(true);
            }}
            className="p-3.5 rounded-xl bg-muted/40 border border-border/40 hover:border-primary/50 cursor-pointer flex items-center justify-between transition-all group/qr"
            title="Click to view dynamic time-refreshing QR pass"
          >
            <div className="space-y-0.5">
              <span className="text-xs font-bold text-foreground block group-hover/qr:text-primary transition-colors">
                Dynamic Security Pass
              </span>
              <span className="text-[11px] text-muted-foreground">Click to display admission QR</span>
            </div>
            <div className="h-10 w-10 rounded-lg bg-white p-1.5 flex items-center justify-center shadow-sm border border-neutral-200 group-hover/qr:scale-105 transition-transform">
              <QrCode className="h-7 w-7 text-neutral-900" />
            </div>
          </div>
        </CardContent>

        {/* Footer Actions */}
        <div className="p-4 bg-muted/30 border-t border-border/40 flex items-center justify-between gap-3">
          <Link href={`/events/${ticket.eventSlug || ticket.eventId}`}>
            <Button size="sm" variant="outline" className="text-xs h-8">
              Event Hub
            </Button>
          </Link>

          {ticket.status === "CONFIRMED" && onCancel && (
            <Button
              size="sm"
              variant="ghost"
              onClick={handleCancelClick}
              disabled={cancelling}
              className="text-xs h-8 text-destructive hover:bg-destructive/10 gap-1.5"
            >
              <XCircle className="h-3.5 w-3.5" />
              {cancelling ? "Cancelling..." : "Cancel Pass"}
            </Button>
          )}
        </div>
      </Card>

      {/* Dynamic QR Modal rendered at root of ticket card */}
      <DynamicQrModal
        ticket={ticket}
        open={showQrModal}
        onClose={() => setShowQrModal(false)}
      />
    </>
  );
}
