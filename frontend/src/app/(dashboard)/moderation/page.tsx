"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { formatDate } from "@/lib/utils";
import { 
  ShieldAlert, 
  CheckCircle2, 
  Trash2, 
  EyeOff, 
  AlertTriangle, 
  Sparkles,
  ArrowLeft,
  User
} from "lucide-react";

interface ModerationFlag {
  id: string;
  postId: string;
  postContent: string;
  postAuthorName: string;
  reportedByName: string;
  reason: string;
  status: string;
  createdAt: string;
}

export default function ModerationConsolePage() {
  const [flags, setFlags] = useState<ModerationFlag[]>([]);
  const [loading, setLoading] = useState(true);
  const [resolvingId, setResolvingId] = useState<string | null>(null);

  const fetchFlags = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get<any>("/moderation/flags");
      if (response.data.success && response.data.data) {
        setFlags(response.data.data.content || []);
      }
    } catch {
      // Fallback demo flagged posts
      setFlags([
        {
          id: "f1",
          postId: "p99",
          postContent: "Check out this free crypto link: http://scam-airdrop.xyz/claim",
          postAuthorName: "SpamBot42",
          reportedByName: "Alex Rivera",
          reason: "Suspected phishing and spam link",
          status: "PENDING",
          createdAt: "2026-08-22T14:00:00Z"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFlags();
  }, []);

  const handleResolve = async (flagId: string, action: "DISMISS" | "REMOVE_POST") => {
    setResolvingId(flagId);
    try {
      await apiClient.post(`/moderation/flags/${flagId}/resolve`, {
        action,
        resolutionNotes: `Resolved as ${action} by moderator`,
      });
      setFlags(prev => prev.filter(f => f.id !== flagId));
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to resolve flag.");
    } finally {
      setResolvingId(null);
    }
  };

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-8 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/40 pb-6">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-3xl font-extrabold tracking-tight">Moderation Console</h1>
            <Badge variant="destructive">{flags.length} Pending</Badge>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Review reported community discussion posts and enforce platform trust and safety guidelines.
          </p>
        </div>
      </div>

      {loading ? (
        <div className="space-y-4">
          {[1, 2].map((i) => (
            <div key={i} className="h-40 rounded-2xl bg-muted/40 animate-pulse border" />
          ))}
        </div>
      ) : flags.length === 0 ? (
        <div className="p-16 text-center border rounded-2xl bg-card space-y-3">
          <CheckCircle2 className="h-10 w-10 text-emerald-500 mx-auto" />
          <h3 className="text-base font-bold">Moderation Queue is Clean</h3>
          <p className="text-xs text-muted-foreground">No pending reports or toxic content flagged for review.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {flags.map((flag) => (
            <Card key={flag.id} className="border-destructive/30 bg-card shadow-sm">
              <CardContent className="p-6 space-y-4">
                {/* Header */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-border/40 pb-3">
                  <div className="flex items-center gap-2">
                    <ShieldAlert className="h-4 w-4 text-destructive" />
                    <span className="font-bold text-xs text-destructive uppercase tracking-wider">Reported Issue</span>
                    <span className="text-xs text-foreground font-semibold">— {flag.reason}</span>
                  </div>
                  <span className="text-[11px] text-muted-foreground">{formatDate(flag.createdAt)}</span>
                </div>

                {/* Post Quote */}
                <div className="p-4 rounded-xl bg-muted/50 border border-border/60 space-y-2">
                  <div className="flex items-center justify-between text-xs text-muted-foreground">
                    <span className="font-semibold text-foreground flex items-center gap-1">
                      <User className="h-3 w-3" />
                      Author: {flag.postAuthorName}
                    </span>
                    <span>Reported by: {flag.reportedByName}</span>
                  </div>
                  <p className="text-xs text-foreground/90 font-mono bg-card/80 p-2.5 rounded-lg border">
                    &ldquo;{flag.postContent}&rdquo;
                  </p>
                </div>

                {/* Actions */}
                <div className="flex justify-end items-center gap-3 pt-2">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleResolve(flag.id, "DISMISS")}
                    disabled={resolvingId === flag.id}
                    className="text-xs h-8"
                  >
                    Dismiss Report
                  </Button>
                  <Button
                    size="sm"
                    variant="destructive"
                    onClick={() => handleResolve(flag.id, "REMOVE_POST")}
                    disabled={resolvingId === flag.id}
                    className="text-xs h-8 gap-1.5 font-semibold"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    Remove Post & Take Down
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
