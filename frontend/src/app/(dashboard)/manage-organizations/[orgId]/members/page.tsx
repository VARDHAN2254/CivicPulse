"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { OrgRole, OrgMemberDto, ApiResponse, PagedResponse, Organization } from "@/types";
import { 
  Users, 
  UserPlus, 
  ShieldCheck, 
  Trash2, 
  ArrowLeft, 
  CheckCircle2, 
  AlertCircle,
  Mail,
  User,
  MoreHorizontal
} from "lucide-react";

export default function OrgMembersPage() {
  const params = useParams();
  const orgId = params.orgId as string;

  const [members, setMembers] = useState<OrgMemberDto[]>([]);
  const [org, setOrg] = useState<Organization | null>(null);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);

  const [email, setEmail] = useState("");
  const [role, setRole] = useState<OrgRole>("MEMBER");
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchMembersAndOrg = async () => {
    setLoading(true);
    try {
      const [membersRes, orgRes] = await Promise.all([
        apiClient.get<ApiResponse<PagedResponse<OrgMemberDto>>>(`/organizations/${orgId}/members`),
        apiClient.get<ApiResponse<Organization>>(`/organizations/${orgId}`),
      ]);

      if (membersRes.data.success && membersRes.data.data) {
        setMembers(membersRes.data.data.content || []);
      }
      if (orgRes.data.success && orgRes.data.data) {
        setOrg(orgRes.data.data);
      }
    } catch {
      // Fallback demo data
      setMembers([
        {
          id: "m1",
          userId: "u1",
          email: "organizer@civicpulse.org",
          fullName: "Sarah Jenkins",
          role: "OWNER",
          joinedAt: "2026-01-15T08:00:00Z"
        },
        {
          id: "m2",
          userId: "u2",
          email: "member@civicpulse.org",
          fullName: "Alex Rivera",
          role: "MEMBER",
          joinedAt: "2026-02-01T10:00:00Z"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMembersAndOrg();
  }, [orgId]);

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);
    setSubmitting(true);

    try {
      const response = await apiClient.post<ApiResponse<OrgMemberDto>>(`/organizations/${orgId}/members`, {
        email: email.trim(),
        role,
      });

      if (response.data.success) {
        setSuccessMsg(`User ${email} added as ${role}`);
        setEmail("");
        setShowAddModal(false);
        fetchMembersAndOrg();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to add member to organization.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdateRole = async (targetUserId: string, newRole: OrgRole) => {
    try {
      await apiClient.put(`/organizations/${orgId}/members/${targetUserId}/role`, {
        role: newRole,
      });
      fetchMembersAndOrg();
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to update role");
    }
  };

  const handleRemoveMember = async (targetUserId: string) => {
    if (!confirm("Are you sure you want to remove this member from the organization?")) return;

    try {
      await apiClient.delete(`/organizations/${orgId}/members/${targetUserId}`);
      fetchMembersAndOrg();
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to remove member");
    }
  };

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-8 max-w-4xl">
      <Link href="/manage-organizations" className="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground">
        <ArrowLeft className="h-3.5 w-3.5" />
        Back to my organizations
      </Link>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/40 pb-6">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-3xl font-extrabold tracking-tight">Organization Roster</h1>
            {org && <Badge variant="outline">{org.name}</Badge>}
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Manage coordinators, organizers, and volunteers assigned to this organization.
          </p>
        </div>

        <Button onClick={() => setShowAddModal(true)} variant="gradient" size="sm" className="gap-2">
          <UserPlus className="h-4 w-4" />
          Add Member
        </Button>
      </div>

      {successMsg && (
        <div className="flex items-center gap-2 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs">
          <CheckCircle2 className="h-4 w-4" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Add Member Modal */}
      {showAddModal && (
        <Card className="border-emerald-500/30 shadow-2xl bg-card">
          <CardHeader>
            <CardTitle className="text-base font-bold">Add Member by Email</CardTitle>
            <CardDescription className="text-xs">
              The user must have an active CivicPulse account registered with this email address.
            </CardDescription>
          </CardHeader>

          <form onSubmit={handleAddMember}>
            <CardContent className="space-y-4">
              {error && (
                <div className="flex items-center gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-xs">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <div className="space-y-2">
                <label className="text-xs font-semibold">User Email Address</label>
                <Input
                  type="email"
                  required
                  placeholder="collaborator@example.org"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <label className="text-xs font-semibold">Assigned Role</label>
                <select
                  value={role}
                  onChange={(e) => setRole(e.target.value as OrgRole)}
                  className="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <option value="MEMBER">Member (Participant)</option>
                  <option value="ORGANIZER">Organizer (Can publish & manage events)</option>
                  <option value="OWNER">Owner (Full admin & roster control)</option>
                </select>
              </div>
            </CardContent>

            <div className="p-4 bg-muted/20 border-t flex justify-end gap-2">
              <Button type="button" variant="ghost" size="sm" onClick={() => setShowAddModal(false)}>
                Cancel
              </Button>
              <Button type="submit" variant="gradient" size="sm" disabled={submitting}>
                {submitting ? "Adding..." : "Add to Roster"}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {/* Members List Table / Cards */}
      <Card className="border-border">
        <CardHeader className="pb-3">
          <CardTitle className="text-base font-bold flex items-center gap-2">
            <Users className="h-4 w-4 text-primary" />
            Active Organization Members ({members.length})
          </CardTitle>
        </CardHeader>

        <CardContent className="p-0">
          <div className="divide-y divide-border">
            {members.map((member) => (
              <div key={member.id} className="p-4 flex items-center justify-between gap-4 hover:bg-muted/30 transition-colors">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full bg-emerald-100 dark:bg-emerald-950/70 text-emerald-700 dark:text-emerald-300 flex items-center justify-center font-bold text-xs border border-emerald-400/30">
                    {member.fullName ? member.fullName.charAt(0).toUpperCase() : "U"}
                  </div>
                  <div>
                    <h4 className="font-semibold text-sm text-foreground">{member.fullName || "User"}</h4>
                    <p className="text-xs text-muted-foreground">{member.email}</p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <Badge variant={member.role === "OWNER" ? "success" : member.role === "ORGANIZER" ? "info" : "secondary"}>
                    {member.role}
                  </Badge>

                  {member.role !== "OWNER" && (
                    <div className="flex items-center gap-1">
                      <select
                        value={member.role}
                        onChange={(e) => handleUpdateRole(member.userId, e.target.value as OrgRole)}
                        className="text-xs h-7 rounded border border-input bg-background px-2 py-0"
                        title="Change member role"
                      >
                        <option value="MEMBER">MEMBER</option>
                        <option value="ORGANIZER">ORGANIZER</option>
                        <option value="OWNER">OWNER</option>
                      </select>

                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleRemoveMember(member.userId)}
                        className="h-8 w-8 text-muted-foreground hover:text-destructive"
                        title="Remove member"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
