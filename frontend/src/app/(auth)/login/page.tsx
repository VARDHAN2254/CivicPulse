"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/useAuth";
import {
  Lock,
  Mail,
  ArrowRight,
  AlertCircle,
  ShieldCheck,
  KeyRound,
  Info
} from "lucide-react";

export default function LoginPage() {
  const { login, verifyMfaLogin, verifyRegistration } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // MFA / Verification Challenge state
  const [challengeMode, setChallengeMode] = useState<"NONE" | "MFA" | "EMAIL_VERIFICATION">("NONE");
  const [challengeId, setChallengeId] = useState("");
  const [challengeEmail, setChallengeEmail] = useState("");
  const [otp, setOtp] = useState("");

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    const result = await login(email, password);

    if (result.mfaRequired && result.challengeId) {
      setChallengeId(result.challengeId);
      setChallengeEmail(result.email || email);
      setChallengeMode("MFA");
      setLoading(false);
      return;
    }

    if (result.verificationRequired && result.challengeId) {
      setChallengeId(result.challengeId);
      setChallengeEmail(result.email || email);
      setChallengeMode("EMAIL_VERIFICATION");
      setLoading(false);
      return;
    }

    if (!result.success) {
      setError(result.message || "Invalid email or password.");
      setLoading(false);
    }
  };

  const handleOtpChallengeSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const cleanOtp = otp.trim().replace(/\D/g, "");
    if (cleanOtp.length !== 8) {
      setError("Please enter the complete 8-digit verification code.");
      return;
    }

    setLoading(true);

    if (challengeMode === "MFA") {
      const res = await verifyMfaLogin(challengeId, challengeEmail, cleanOtp);
      if (!res.success) {
        setError(res.message || "Invalid or expired MFA code.");
        setLoading(false);
      }
    } else if (challengeMode === "EMAIL_VERIFICATION") {
      const res = await verifyRegistration(challengeId, challengeEmail, cleanOtp);
      if (!res.success) {
        setError(res.message || "Invalid or expired verification code.");
        setLoading(false);
      }
    }
  };

  const handleDemoFill = (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword("Password123!");
  };

  return (
    <Card className="border-border shadow-2xl overflow-hidden max-w-md mx-auto">
      <CardHeader className="space-y-1">
        <div className="flex items-center justify-between">
          <Badge variant="outline" className="text-[11px] gap-1 font-semibold">
            <ShieldCheck className="h-3 w-3 text-primary" />
            {challengeMode === "MFA"
              ? "Multi-Factor Authentication"
              : challengeMode === "EMAIL_VERIFICATION"
              ? "Email Verification Required"
              : "Zero-Trust Security"}
          </Badge>
        </div>

        <CardTitle className="text-2xl font-bold tracking-tight">
          {challengeMode === "NONE" && "Sign In to CivicPulse"}
          {challengeMode === "MFA" && "Enter MFA Security Code"}
          {challengeMode === "EMAIL_VERIFICATION" && "Verify Account Email"}
        </CardTitle>
        <CardDescription className="text-xs">
          {challengeMode === "NONE" && "Enter your email and password to access your community account."}
          {challengeMode === "MFA" && `Enter the 8-digit security code dispatched to ${challengeEmail}.`}
          {challengeMode === "EMAIL_VERIFICATION" && `Your email address is unverified. Enter the 8-digit activation code sent to ${challengeEmail}.`}
        </CardDescription>
      </CardHeader>

      {/* STANDARD LOGIN FORM */}
      {challengeMode === "NONE" && (
        <form onSubmit={handlePasswordSubmit}>
          <CardContent className="space-y-4">
            {error && (
              <div className="flex items-start gap-2 p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-xs animate-in fade-in">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="leading-snug">{error}</span>
              </div>
            )}

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
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                  <Lock className="h-3.5 w-3.5 text-primary" />
                  Password
                </label>
                <Link href="/forgot-password" className="text-xs text-primary hover:underline font-semibold">
                  Forgot password?
                </Link>
              </div>
              <Input
                type="password"
                placeholder="••••••••••••"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                className="h-10 text-sm"
              />
            </div>

            {/* Quick Demo Fill Accounts */}
            <div className="pt-2 border-t border-border/40">
              <span className="text-[11px] text-muted-foreground font-medium block mb-2">
                Quick Demo Accounts (Preloaded):
              </span>
              <div className="grid grid-cols-2 gap-1.5">
                <button
                  type="button"
                  onClick={() => handleDemoFill("organizer@civicpulse.org")}
                  className="text-[11px] px-2 py-1.5 rounded-lg bg-muted/60 hover:bg-muted text-muted-foreground hover:text-foreground text-left transition-colors border border-border/40"
                >
                  Organizer Account
                </button>
                <button
                  type="button"
                  onClick={() => handleDemoFill("member@civicpulse.org")}
                  className="text-[11px] px-2 py-1.5 rounded-lg bg-muted/60 hover:bg-muted text-muted-foreground hover:text-foreground text-left transition-colors border border-border/40"
                >
                  Member Account
                </button>
                <button
                  type="button"
                  onClick={() => handleDemoFill("admin@civicpulse.org")}
                  className="text-[11px] px-2 py-1.5 rounded-lg bg-muted/60 hover:bg-muted text-muted-foreground hover:text-foreground text-left transition-colors border border-border/40"
                >
                  Admin (Enforced MFA)
                </button>
                <button
                  type="button"
                  onClick={() => handleDemoFill("moderator@civicpulse.org")}
                  className="text-[11px] px-2 py-1.5 rounded-lg bg-muted/60 hover:bg-muted text-muted-foreground hover:text-foreground text-left transition-colors border border-border/40"
                >
                  Moderator Account
                </button>
              </div>
            </div>
          </CardContent>

          <CardFooter className="flex flex-col space-y-3 bg-muted/10 border-t border-border/40 pt-4">
            <Button type="submit" variant="gradient" className="w-full gap-2 font-medium" disabled={loading}>
              {loading ? "Authenticating..." : "Sign In"}
              {!loading && <ArrowRight className="h-4 w-4" />}
            </Button>

            <p className="text-xs text-center text-muted-foreground">
              Don&apos;t have an account yet?{" "}
              <Link href="/register" className="text-primary hover:underline font-semibold">
                Create an account
              </Link>
            </p>
          </CardFooter>
        </form>
      )}

      {/* STEP-UP MFA / EMAIL VERIFICATION CHALLENGE */}
      {challengeMode !== "NONE" && (
        <form onSubmit={handleOtpChallengeSubmit}>
          <CardContent className="space-y-4">
            {error && (
              <div className="flex items-start gap-2 p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-xs animate-in fade-in">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                <span className="leading-snug">{error}</span>
              </div>
            )}

            <div className="p-3.5 rounded-xl bg-primary/5 border border-primary/20 text-xs space-y-1.5">
              <div className="flex items-center gap-1.5 text-primary font-semibold">
                <Info className="h-4 w-4 shrink-0" />
                <span>Check Your Email Inbox</span>
              </div>
              <p className="text-muted-foreground leading-relaxed">
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
                  8-Digit Verification Code
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
          </CardContent>

          <CardFooter className="flex flex-col space-y-3 bg-muted/10 border-t border-border/40 pt-4">
            <Button type="submit" variant="gradient" className="w-full gap-2 font-medium" disabled={loading || otp.length !== 8}>
              {loading ? "Verifying Code..." : "Verify & Sign In"}
            </Button>

            <button
              type="button"
              onClick={() => {
                setChallengeMode("NONE");
                setOtp("");
                setError(null);
              }}
              className="text-xs text-muted-foreground hover:text-foreground text-center"
            >
              Cancel and return to password login
            </button>
          </CardFooter>
        </form>
      )}
    </Card>
  );
}
