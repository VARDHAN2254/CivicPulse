"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { Organization, ApiResponse } from "@/types";
import { Building2, PlusCircle, Users, CheckCircle2, Globe, Shield, ArrowRight, AlertCircle } from "lucide-react";

export default function MyOrganizationsPage() {
  const [organizations, setOrganizations] = useState<(Organization & { currentUserRole?: string; memberCount?: number })[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Form states
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [website, setWebsite] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  const fetchMyOrganizations = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get<ApiResponse<any[]>>("/organizations/my");
      if (response.data.success && response.data.data) {
        setOrganizations(response.data.data);
      }
    } catch {
      // Fallback preview
      setOrganizations([
        {
          id: "b0000000-0000-0000-0000-000000000001",
          name: "Green Earth Volunteers",
          slug: "green-earth-volunteers",
          description: "Environmental conservation and cleanup drives.",
          website: "https://greenearth.org",
          contactEmail: "contact@greenearth.org",
          isVerified: true,
          currentUserRole: "OWNER",
          memberCount: 8,
          createdAt: "2026-01-15T08:00:00Z"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMyOrganizations();
  }, []);

  const handleCreateOrg = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError(null);
    setCreating(true);

    try {
      const response = await apiClient.post<ApiResponse<Organization>>("/organizations", {
        name,
        description,
        website,
        contactEmail,
      });

      if (response.data.success) {
        setShowCreateModal(false);
        setName("");
        setDescription("");
        setWebsite("");
        setContactEmail("");
        fetchMyOrganizations();
      }
    } catch (err: any) {
      setCreateError(err.response?.data?.message || "Failed to create organization.");
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-8 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/40 pb-6">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">My Organizations</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Manage your community hubs, member rosters, and event publishing permissions.
          </p>
        </div>

        <Button onClick={() => setShowCreateModal(true)} variant="gradient" size="sm" className="gap-2">
          <PlusCircle className="h-4 w-4" />
          Create Organization
        </Button>
      </div>

      {/* Create Modal / Card */}
      {showCreateModal && (
        <Card className="border-emerald-500/30 bg-card shadow-2xl animate-in fade-in">
          <CardHeader>
            <CardTitle className="text-lg font-bold">Register New Community Organization</CardTitle>
            <CardDescription className="text-xs">
              As creator, you will be assigned as the primary Owner of this organization.
            </CardDescription>
          </CardHeader>

          <form onSubmit={handleCreateOrg}>
            <CardContent className="space-y-4">
              {createError && (
                <div className="flex items-center gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-xs">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  <span>{createError}</span>
                </div>
              )}

              <div className="space-y-2">
                <label className="text-xs font-semibold">Organization Name</label>
                <Input
                  type="text"
                  required
                  placeholder="e.g. City Tech Collaborative"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold">Description / Mission</label>
                <textarea
                  rows={3}
                  required
                  placeholder="What is your organization's mission and civic focus?"
                  className="flex w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-xs font-semibold">Website (Optional)</label>
                  <Input
                    type="url"
                    placeholder="https://myclub.org"
                    value={website}
                    onChange={(e) => setWebsite(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-semibold">Contact Email</label>
                  <Input
                    type="email"
                    placeholder="contact@myclub.org"
                    value={contactEmail}
                    onChange={(e) => setContactEmail(e.target.value)}
                  />
                </div>
              </div>
            </CardContent>

            <CardFooter className="flex justify-end gap-2 border-t pt-4">
              <Button type="button" variant="ghost" size="sm" onClick={() => setShowCreateModal(false)}>
                Cancel
              </Button>
              <Button type="submit" variant="gradient" size="sm" disabled={creating}>
                {creating ? "Creating..." : "Create Organization"}
              </Button>
            </CardFooter>
          </form>
        </Card>
      )}

      {/* Organizations Grid */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="h-44 rounded-xl bg-muted animate-pulse" />
          <div className="h-44 rounded-xl bg-muted animate-pulse" />
        </div>
      ) : organizations.length === 0 ? (
        <div className="p-12 text-center border rounded-2xl bg-card space-y-3">
          <Building2 className="h-10 w-10 text-muted-foreground mx-auto" />
          <h3 className="text-base font-semibold">You don&apos;t belong to any organizations yet</h3>
          <p className="text-xs text-muted-foreground">Create an organization to start hosting events and managing attendees.</p>
          <Button onClick={() => setShowCreateModal(true)} variant="gradient" size="sm" className="mt-2">
            Create First Organization
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {organizations.map((org) => (
            <Card key={org.id} className="glow-card border-border/80 flex flex-col justify-between">
              <CardContent className="p-6 space-y-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="h-12 w-12 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white flex items-center justify-center font-bold text-lg shadow-sm">
                      {org.name.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <h3 className="font-bold text-base text-foreground">{org.name}</h3>
                      <span className="text-xs text-muted-foreground font-mono">@{org.slug}</span>
                    </div>
                  </div>

                  <Badge variant="success" className="text-xs">
                    {org.currentUserRole || "MEMBER"}
                  </Badge>
                </div>

                <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
                  {org.description}
                </p>

                <div className="pt-2 border-t flex items-center justify-between text-xs text-muted-foreground">
                  <div className="flex items-center gap-1.5 font-medium">
                    <Users className="h-3.5 w-3.5 text-primary" />
                    <span>{org.memberCount || 1} Member{(org.memberCount || 1) > 1 ? "s" : ""}</span>
                  </div>
                  {org.isVerified && (
                    <span className="flex items-center gap-1 text-emerald-500 font-semibold">
                      <CheckCircle2 className="h-3.5 w-3.5" />
                      Verified Hub
                    </span>
                  )}
                </div>
              </CardContent>

              <div className="p-4 bg-muted/30 border-t flex items-center justify-between gap-2">
                <Link href={`/organizations/${org.slug}`} className="flex-1">
                  <Button variant="outline" size="sm" className="w-full text-xs h-8">
                    Public View
                  </Button>
                </Link>
                {['OWNER', 'ORGANIZER'].includes(org.currentUserRole || '') && (
                  <Link href={`/manage-organizations/${org.id}/members`} className="flex-1">
                    <Button variant="gradient" size="sm" className="w-full text-xs h-8 gap-1.5">
                      <Users className="h-3.5 w-3.5" />
                      Manage Roster
                    </Button>
                  </Link>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
