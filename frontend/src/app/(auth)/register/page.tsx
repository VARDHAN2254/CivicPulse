"use client";

import React, { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/useAuth";
import { UserRole, ApiResponse } from "@/types";
import { apiClient } from "@/lib/api-client";
import {
  Lock,
  Mail,
  User,
  Phone,
  ArrowRight,
  AlertCircle,
  KeyRound,
  Clock,
  RefreshCw,
  Building2,
  Heart,
  ShieldCheck,
  Info
} from "lucide-react";

export default function RegisterPage() {
  const { register, verifyRegistration } = useAuth();
  const [step, setStep] = useState<"FORM" | "OTP">("FORM");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [role, setRole] = useState<UserRole>("MEMBER");
  const [phoneNumber, setPhoneNumber] = useState("");

  const [challengeId, setChallengeId] = useState("");
  const [otp, setOtp] = useState("");

  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);

  // Timer & Cooldown
  const [secondsRemaining, setSecondsRemaining] = useState(300);
  const [cooldownRemaining, setCooldownRemaining] = useState(60);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (step === "OTP") {
      setSecondsRemaining(300);
      setCooldownRemaining(60);

      timerRef.current = setInterval(() => {
        setSecondsRemaining((prev) => Math.max(0, prev - 1));
        setCooldownRemaining((prev) => Math.max(0, prev - 1));
      }, 1000);
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [step]);

  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password.length < 8) {
      setError("Password must be at least 8 characters long.");
      return;
    }

    setLoading(true);
    const result = await register(email, password, fullName, role, phoneNumber);
    if (result.success && result.verificationRequired) {
      if (result.challengeId) setChallengeId(result.challengeId);
      setStep("OTP");
      setLoading(false);
    } else if (!result.success) {
      setError(result.message || "Failed to create account. Please check your information.");
      setLoading(false);
    }
  };

  const handleOtpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const cleanOtp = otp.trim().replace(/\D/g, "");
    if (cleanOtp.length !== 8) {
      setError("Please enter the complete 8-digit verification code.");
      return;
    }

    setLoading(true);
    const result = await verifyRegistration(challengeId, email, cleanOtp);
    if (!result.success) {
      setError(result.message || "Invalid verification code.");
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (cooldownRemaining > 0) return;
    setError(null);
    setResending(true);

    try {
      const res = await apiClient.post<ApiResponse<any>>("/auth/resend-otp", {
        challengeId,
        email: email.trim(),
        purpose: "REGISTRATION_VERIFICATION",
      });

      if (res.data.success) {
        if (res.data.data?.challengeId) setChallengeId(res.data.data.challengeId);
        setCooldownRemaining(60);
        setSecondsRemaining(300);
        setOtp("");
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to resend verification code.");
    } finally {
      setResending(false);
    }
  };

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s < 10 ? "0" : ""}${s}`;
  };

  return (
    <Card className="border-border shadow-2xl overflow-hidden max-w-md mx-auto">
      {/* Progress Bar */}
      <div className="h-1.5 w-full bg-muted">
        <div
          className="h-full bg-primary transition-all duration-300"
          style={{ width: step === "FORM" ? "50%" : "100%" }}
        />
      </div>

      <CardHeader className="space-y-1 pb-4">
        <div className="flex items-center justify-between">
          <Badge variant="outline" className="text-[11px] gap-1 font-semibold">
            <ShieldCheck className="h-3 w-3 text-primary" />
            Verified Citizen Registration
          </Badge>
          <span className="text-[11px] font-mono text-muted-foreground">
            {step === "FORM" ? "Step 1 of 2" : "Step 2 of 2"}
          </span>
        </div>
        <CardTitle className="text-xl font-bold tracking-tight">
          {step === "FORM" ? "Create your account" : "Verify your email"}
        </CardTitle>
        <CardDescription className="text-xs">
          {step === "FORM"
            ? "Join CivicPulse to explore local events, join waitlists, or host community drives."
            : `Enter the 8-digit verification code sent to ${email}.`}
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-4 pt-0">
        {error && (
          <div className="flex items-start gap-2 p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-xs animate-in fade-in">
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
            <span className="leading-snug">{error}</span>
          </div>
        )}

        {/* STEP 1: REGISTRATION FORM */}
        {step === "FORM" && (
          <form onSubmit={handleRegisterSubmit} className="space-y-3.5">
            {/* Role Selector */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground">I want to join as:</label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setRole("MEMBER")}
                  className={`flex items-center gap-2 p-2.5 rounded-xl border text-xs font-medium transition-all ${
                    role === "MEMBER"
                      ? "border-emerald-600 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-semibold"
                      : "border-border bg-card hover:bg-muted text-muted-foreground"
                  }`}
                >
                  <Heart className="h-4 w-4" />
                  Community Member
                </button>

                <button
                  type="button"
                  onClick={() => setRole("ORGANIZER")}
                  className={`flex items-center gap-2 p-2.5 rounded-xl border text-xs font-medium transition-all ${
                    role === "ORGANIZER"
                      ? "border-emerald-600 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-semibold"
                      : "border-border bg-card hover:bg-muted text-muted-foreground"
                  }`}
                >
                  <Building2 className="h-4 w-4" />
                  Event Organizer / NGO
                </button>
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <User className="h-3.5 w-3.5 text-primary" />
                Full Name
              </label>
              <Input
                type="text"
                placeholder="e.g. Jane Doe"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="h-10 text-sm"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <Mail className="h-3.5 w-3.5 text-primary" />
                Email Address
              </label>
              <Input
                type="email"
                placeholder="citizen@civicpulse.org"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                className="h-10 text-sm"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <Lock className="h-3.5 w-3.5 text-primary" />
                Password (min 8 chars)
              </label>
              <Input
                type="password"
                placeholder="••••••••••••"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
                className="h-10 text-sm"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <Phone className="h-3.5 w-3.5 text-muted-foreground" />
                Phone Number (Optional)
              </label>
              <Input
                type="tel"
                placeholder="+1 (555) 000-0000"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                className="h-10 text-sm"
              />
            </div>

            <Button type="submit" variant="gradient" className="w-full gap-2 font-medium mt-2" disabled={loading}>
              {loading ? "Creating Account..." : "Create Account & Get Code"}
              {!loading && <ArrowRight className="h-4 w-4" />}
            </Button>
          </form>
        )}

        {/* STEP 2: 8-DIGIT OTP VERIFICATION */}
        {step === "OTP" && (
          <form onSubmit={handleOtpSubmit} className="space-y-4">
            <div className="p-3.5 rounded-xl bg-primary/5 border border-primary/20 space-y-1.5 text-xs">
              <div className="flex items-center justify-between font-semibold text-foreground">
                <span className="flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-primary" />
                  Code Expires In:
                </span>
                <span className={`font-mono font-bold ${secondsRemaining < 60 ? "text-destructive" : "text-primary"}`}>
                  {formatTime(secondsRemaining)}
                </span>
              </div>
              <p className="text-[11px] text-muted-foreground">
                An 8-digit verification code has been dispatched.
              </p>
              <div className="pt-1 text-[11px] text-primary/80 font-medium flex items-center gap-1">
                <span>💡 <strong>Local Development:</strong> Inspect dispatched emails in Mailpit at <a href="http://localhost:8025" target="_blank" rel="noreferrer" className="underline font-mono">http://localhost:8025</a>.</span>
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-foreground flex items-center justify-between">
                <span className="flex items-center gap-1.5">
                  <KeyRound className="h-3.5 w-3.5 text-primary" />
                  8-Digit Activation Code
                </span>
                <span className="text-[11px] text-muted-foreground font-mono">
                  {otp.length}/8 Digits
                </span>
              </label>
              <Input
                type="text"
                inputMode="numeric"
                maxLength={8}
                placeholder="12345678"
                autoFocus
                required
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 8))}
                className="h-12 text-center text-xl font-mono tracking-widest font-bold"
              />
            </div>

            <Button type="submit" variant="gradient" className="w-full font-medium" disabled={loading || otp.length !== 8}>
              {loading ? "Activating Account..." : "Verify & Enter CivicPulse"}
            </Button>

            <div className="flex items-center justify-between text-xs pt-1">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={handleResendOtp}
                disabled={resending || cooldownRemaining > 0}
                className="h-8 text-xs gap-1 text-muted-foreground hover:text-foreground"
              >
                <RefreshCw className={`h-3 w-3 ${resending ? "animate-spin" : ""}`} />
                {cooldownRemaining > 0 ? `Resend in ${cooldownRemaining}s` : "Resend Code"}
              </Button>

              <button
                type="button"
                onClick={() => setStep("FORM")}
                className="text-xs text-muted-foreground hover:text-foreground underline underline-offset-4"
              >
                Edit registration
              </button>
            </div>
          </form>
        )}
      </CardContent>

      <CardFooter className="flex justify-center border-t border-border/40 pt-4 bg-muted/20">
        <p className="text-xs text-muted-foreground">
          Already have an account?{" "}
          <Link href="/login" className="text-primary hover:underline font-semibold">
            Sign in
          </Link>
        </p>
      </CardFooter>
    </Card>
  );
}
