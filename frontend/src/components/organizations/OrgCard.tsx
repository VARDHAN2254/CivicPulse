import React from "react";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Organization } from "@/types";
import { Building2, Users, CheckCircle2, Globe, Mail, ArrowRight } from "lucide-react";

export function OrgCard({ org }: { org: Organization & { memberCount?: number; currentUserRole?: string } }) {
  return (
    <Card className="glow-card border-border/80 flex flex-col justify-between overflow-hidden group">
      <CardContent className="p-6 space-y-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="h-12 w-12 rounded-xl bg-gradient-to-tr from-emerald-500/20 to-teal-500/20 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold text-lg border border-emerald-500/30">
              {org.name.charAt(0).toUpperCase()}
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h3 className="font-bold text-base text-foreground group-hover:text-primary transition-colors">
                  {org.name}
                </h3>
                {org.isVerified && (
                  <span title="Verified Organization">
                    <CheckCircle2 className="h-4 w-4 text-emerald-500 fill-emerald-500/20 shrink-0" />
                  </span>
                )}
              </div>
              <span className="text-xs text-muted-foreground font-mono">@{org.slug}</span>
            </div>
          </div>

          {org.currentUserRole && (
            <Badge variant="success" className="text-[10px]">
              {org.currentUserRole}
            </Badge>
          )}
        </div>

        <p className="text-xs text-muted-foreground line-clamp-3 leading-relaxed">
          {org.description || "Active community organization coordinating local events and volunteers."}
        </p>

        <div className="pt-2 border-t border-border/40 flex items-center justify-between text-xs text-muted-foreground">
          <div className="flex items-center gap-1.5 font-medium">
            <Users className="h-3.5 w-3.5 text-primary" />
            <span>{org.memberCount || 1} Member{(org.memberCount || 1) > 1 ? "s" : ""}</span>
          </div>

          {org.website && (
            <span className="flex items-center gap-1 hover:text-foreground truncate max-w-[150px]">
              <Globe className="h-3.5 w-3.5 text-primary" />
              <span>{org.website.replace(/^https?:\/\//, '')}</span>
            </span>
          )}
        </div>
      </CardContent>

      <div className="p-4 bg-muted/30 border-t border-border/40 flex items-center justify-between">
        <Link href={`/organizations/${org.slug}`} className="w-full">
          <Button size="sm" variant="outline" className="w-full text-xs h-8 gap-1.5 justify-center">
            View Hub
            <ArrowRight className="h-3.5 w-3.5" />
          </Button>
        </Link>
      </div>
    </Card>
  );
}
