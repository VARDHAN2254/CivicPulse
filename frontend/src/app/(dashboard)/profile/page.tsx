"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { authStorage } from "@/lib/auth";
import { User, ApiResponse, UserDto } from "@/types";
import { User as UserIcon, Mail, Phone, Lock, Save, CheckCircle2, AlertCircle, Shield } from "lucide-react";

export default function ProfilePage() {
  const [user, setUser] = useState<User | null>(null);
  const [fullName, setFullName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [bio, setBio] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  
  const [profileSuccess, setProfileSuccess] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [loadingPassword, setLoadingPassword] = useState(false);

  useEffect(() => {
    const currentUser = authStorage.getUser();
    if (currentUser) {
      setUser(currentUser);
      setFullName(currentUser.fullName || "");
      setPhoneNumber(currentUser.phoneNumber || "");
      setBio(currentUser.bio || "");
    }
  }, []);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileError(null);
    setProfileSuccess(false);
    setLoadingProfile(true);

    try {
      const response = await apiClient.put<ApiResponse<UserDto>>("/users/profile", {
        fullName,
        phoneNumber,
        bio,
      });

      if (response.data.success && response.data.data) {
        const updated = response.data.data as User;
        authStorage.setUser(updated);
        setUser(updated);
        setProfileSuccess(true);
      }
    } catch (err: any) {
      setProfileError(err.response?.data?.message || "Failed to update profile.");
    } finally {
      setLoadingProfile(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(false);

    if (newPassword.length < 8) {
      setPasswordError("New password must be at least 8 characters long.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setPasswordError("New passwords do not match.");
      return;
    }

    setLoadingPassword(true);
    try {
      const response = await apiClient.post<ApiResponse<void>>("/users/change-password", {
        currentPassword,
        newPassword,
      });

      if (response.data.success) {
        setPasswordSuccess(true);
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
      }
    } catch (err: any) {
      setPasswordError(err.response?.data?.message || "Failed to change password. Check your current password.");
    } finally {
      setLoadingPassword(false);
    }
  };

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 max-w-4xl space-y-8">
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight">Account & Profile Settings</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Manage your personal details, community role, and security credentials.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Profile Card Summary */}
        <Card className="md:col-span-1 border-border/80 h-fit">
          <CardHeader className="text-center pb-2">
            <div className="mx-auto h-20 w-20 rounded-full bg-emerald-100 dark:bg-emerald-950/80 text-emerald-700 dark:text-emerald-300 flex items-center justify-center font-bold text-2xl border-2 border-emerald-400/40 mb-3 shadow-md">
              {fullName ? fullName.charAt(0).toUpperCase() : "U"}
            </div>
            <CardTitle className="text-lg font-bold">{fullName || "User"}</CardTitle>
            <CardDescription className="text-xs truncate">{user?.email}</CardDescription>
          </CardHeader>

          <CardContent className="space-y-3 pt-2 text-xs">
            <div className="flex items-center justify-between py-2 border-b">
              <span className="text-muted-foreground">Platform Role</span>
              <Badge variant="success">{user?.role || "MEMBER"}</Badge>
            </div>
            <div className="flex items-center justify-between py-2 border-b">
              <span className="text-muted-foreground">Account Status</span>
              <Badge variant="outline" className="text-emerald-500 border-emerald-500/40">Active</Badge>
            </div>
            <div className="flex items-center justify-between py-2">
              <span className="text-muted-foreground">Member Since</span>
              <span className="font-medium text-foreground">2026</span>
            </div>
          </CardContent>
        </Card>

        {/* Profile Edit & Security Tabs */}
        <div className="md:col-span-2 space-y-6">
          {/* Profile Form */}
          <Card className="border-border/80">
            <CardHeader>
              <CardTitle className="text-lg font-bold flex items-center gap-2">
                <UserIcon className="h-4 w-4 text-primary" />
                Personal Information
              </CardTitle>
              <CardDescription className="text-xs">
                Update how your name and contact appear across organizations and events.
              </CardDescription>
            </CardHeader>

            <form onSubmit={handleUpdateProfile}>
              <CardContent className="space-y-4">
                {profileSuccess && (
                  <div className="flex items-center gap-2 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs">
                    <CheckCircle2 className="h-4 w-4" />
                    <span>Profile information successfully saved.</span>
                  </div>
                )}
                {profileError && (
                  <div className="flex items-center gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-xs">
                    <AlertCircle className="h-4 w-4" />
                    <span>{profileError}</span>
                  </div>
                )}

                <div className="space-y-2">
                  <label className="text-xs font-semibold">Full Name</label>
                  <Input
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-semibold">Phone Number</label>
                  <Input
                    type="tel"
                    placeholder="+1 (555) 000-0000"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-semibold">Bio / Community Interests</label>
                  <textarea
                    rows={3}
                    placeholder="Tell local organizers about your skills and interests..."
                    className="flex w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    value={bio}
                    onChange={(e) => setBio(e.target.value)}
                  />
                </div>
              </CardContent>

              <CardFooter className="border-t pt-4">
                <Button type="submit" variant="gradient" size="sm" className="gap-2 ml-auto" disabled={loadingProfile}>
                  <Save className="h-3.5 w-3.5" />
                  {loadingProfile ? "Saving..." : "Save Changes"}
                </Button>
              </CardFooter>
            </form>
          </Card>

          {/* Change Password Form */}
          <Card className="border-border/80">
            <CardHeader>
              <CardTitle className="text-lg font-bold flex items-center gap-2">
                <Shield className="h-4 w-4 text-primary" />
                Security & Password
              </CardTitle>
              <CardDescription className="text-xs">
                Ensure your account is protected by using a strong, unique password.
              </CardDescription>
            </CardHeader>

            <form onSubmit={handleChangePassword}>
              <CardContent className="space-y-4">
                {passwordSuccess && (
                  <div className="flex items-center gap-2 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs">
                    <CheckCircle2 className="h-4 w-4" />
                    <span>Password updated successfully. Other active sessions were invalidated.</span>
                  </div>
                )}
                {passwordError && (
                  <div className="flex items-center gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-xs">
                    <AlertCircle className="h-4 w-4" />
                    <span>{passwordError}</span>
                  </div>
                )}

                <div className="space-y-2">
                  <label className="text-xs font-semibold">Current Password</label>
                  <Input
                    type="password"
                    required
                    placeholder="••••••••••••"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <label className="text-xs font-semibold">New Password</label>
                    <Input
                      type="password"
                      required
                      placeholder="••••••••••••"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-semibold">Confirm New Password</label>
                    <Input
                      type="password"
                      required
                      placeholder="••••••••••••"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                    />
                  </div>
                </div>
              </CardContent>

              <CardFooter className="border-t pt-4">
                <Button type="submit" variant="outline" size="sm" className="gap-2 ml-auto" disabled={loadingPassword}>
                  <Lock className="h-3.5 w-3.5" />
                  {loadingPassword ? "Updating..." : "Update Password"}
                </Button>
              </CardFooter>
            </form>
          </Card>
        </div>
      </div>
    </div>
  );
}
