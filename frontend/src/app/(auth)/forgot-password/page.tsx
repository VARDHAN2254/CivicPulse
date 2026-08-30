"use client";

import React, { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { OtpChallengeResponse, VerifyOtpResponse, ApiResponse } from "@/types";
import {
  Mail,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  KeyRound,
  Lock,
  Clock,
  RefreshCw,
  ShieldCheck,
  Info
} from "lucide-react";

type RecoveryStep = "EMAIL" | "OTP" | "NEW_PASSWORD" | "SUCCESS";

export default function ForgotPasswordPage() {
  const [step, setStep] = useState<RecoveryStep>("EMAIL");
  const [email, setEmail] = useState("");
  const [challengeId, setChallengeId] = useState<string>("");
  const [otp, setOtp] = useState("");
  const [resetAuthToken, setResetAuthToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Timer & Cooldown
  const [secondsRemaining, setSecondsRemaining] = useState(300); // 5 mins
  const [cooldownRemaining, setCooldownRemaining] = useState(60); // 60s cooldown
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (step === "OTP") {
      setSecondsRemaining(300);
      setCooldownRemaining(60);

      // Start countdown
      timerRef.current = setInterval(() => {
        setSecondsRemaining((prev) => Math.max(0, prev - 1));
        setCooldownRemaining((prev) => Math.max(0, prev - 1));
      }, 1000);
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [step]);

  // Step 1: Request Password Reset OTP
  const handleRequestOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const response = await apiClient.post<ApiResponse<OtpChallengeResponse>>("/auth/forgot-password", {
        email: email.trim(),
      });

      if (response.data.success) {
        const challenge = response.data.data;
        if (challenge?.challengeId) {
          setChallengeId(challenge.challengeId);
        }
        setStep("OTP");
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to process request. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Verify 8-Digit OTP
  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const cleanOtp = otp.trim().replace(/\D/g, "");
    if (cleanOtp.length !== 8) {
      setError("Please enter the complete 8-digit verification code.");
      return;
    }

    setLoading(true);
    try {
      const response = await apiClient.post<ApiResponse<VerifyOtpResponse>>("/auth/verify-otp", {
        challengeId,
        email: email.trim(),
        otp: cleanOtp,
        purpose: "PASSWORD_RESET",
      });

      if (response.data.success && response.data.data?.resetAuthToken) {
        setResetAuthToken(response.data.data.resetAuthToken);
        setStep("NEW_PASSWORD");
      } else {
        setError(response.data.message || "Invalid OTP code.");
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "Invalid or expired verification code.");
    } finally {
      setLoading(false);
    }
  };

  // Resend OTP
  const handleResendOtp = async () => {
    if (cooldownRemaining > 0) return;
    setError(null);
    setResending(true);

    try {
      const response = await apiClient.post<ApiResponse<OtpChallengeResponse>>("/auth/resend-otp", {
        challengeId,
        email: email.trim(),
        purpose: "PASSWORD_RESET",
      });

      if (response.data.success) {
        const newChallenge = response.data.data;
        if (newChallenge?.challengeId) {
          setChallengeId(newChallenge.challengeId);
        }
        setCooldownRemaining(60);
        setSecondsRemaining(300);
        setOtp("");
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to resend code.");
    } finally {
      setResending(false);
    }
  };

  // Step 3: Set New Password
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (newPassword.length < 8) {
      setError("Password must be at least 8 characters long.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      const response = await apiClient.post("/auth/reset-password", {
        resetAuthToken,
        newPassword,
      });

      if (response.data.success) {
        setStep("SUCCESS");
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to update password. Authorization may have expired.");
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s < 10 ? "0" : ""}${s}`;
  };

  return (
    <Card className="border-border shadow-2xl overflow-hidden max-w-md mx-auto">
      {/* Progress Indicator */}
      <div className="h-1.5 w-full bg-muted">
        <div
          className="h-full bg-primary transition-all duration-300"
          style={{
            width:
              step === "EMAIL"
                ? "25%"
                : step === "OTP"
                ? "60%"
                : step === "NEW_PASSWORD"
                ? "90%"
                : "100%",
          }}
        />
      </div>

      <CardHeader className="space-y-1 pb-4">
        <div className="flex items-center justify-between">
          <Badge variant="outline" className="text-[11px] gap-1 font-semibold">
            <ShieldCheck className="h-3 w-3 text-primary" />
            Zero-Trust Account Recovery
          </Badge>
          <span className="text-[11px] font-mono text-muted-foreground">
            {step === "EMAIL" ? "Step 1/3" : step === "OTP" ? "Step 2/3" : step === "NEW_PASSWORD" ? "Step 3/3" : "Done"}
          </span>
        </div>
        <CardTitle className="text-xl font-bold tracking-tight">
          {step === "EMAIL" && "Reset Your Password"}
          {step === "OTP" && "Enter 8-Digit Verification Code"}
          {step === "NEW_PASSWORD" && "Create New Password"}
          {step === "SUCCESS" && "Password Reset Complete"}
        </CardTitle>
        <CardDescription className="text-xs">
          {step === "EMAIL" && "Enter your registered email address to receive a secure one-time verification code."}
          {step === "OTP" && `We sent a single-use verification code to ${email || "your email"}.`}
          {step === "NEW_PASSWORD" && "Your identity is verified. Enter your new account password."}
          {step === "SUCCESS" && "Your password has been changed and all previous sessions revoked."}
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-4 pt-0">
        {error && (
          <div className="flex items-start gap-2 p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-xs animate-in fade-in">
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
            <span className="leading-snug">{error}</span>
          </div>
        )}

        {/* STEP 1: EMAIL ENTRY */}
        {step === "EMAIL" && (
          <form onSubmit={handleRequestOtp} className="space-y-4">
            <div className="space-y-2">
              <label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <Mail className="h-3.5 w-3.5 text-primary" />
                Registered Account Email
              </label>
              <Input
                type="email"
                placeholder="citizen@civicpulse.org"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="h-10 text-sm"
              />
            </div>

            <Button type="submit" variant="gradient" className="w-full font-medium" disabled={loading}>
              {loading ? "Generating Verification Code..." : "Send Verification Code"}
            </Button>
          </form>
        )}

        {/* STEP 2: 8-DIGIT OTP ENTRY */}
        {step === "OTP" && (
          <form onSubmit={handleVerifyOtp} className="space-y-4">
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
              <p className="text-[11px] text-muted-foreground leading-relaxed">
                If an account with that email exists, an 8-digit verification code has been dispatched.
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

            <Button type="submit" variant="gradient" className="w-full font-medium" disabled={loading || otp.length !== 8}>
              {loading ? "Verifying Code..." : "Verify Code & Proceed"}
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
                onClick={() => setStep("EMAIL")}
                className="text-xs text-muted-foreground hover:text-foreground underline underline-offset-4"
              >
                Change email
              </button>
            </div>
          </form>
        )}

        {/* STEP 3: CREATE NEW PASSWORD */}
        {step === "NEW_PASSWORD" && (
          <form onSubmit={handleResetPassword} className="space-y-4">
            <div className="space-y-2">
              <label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <Lock className="h-3.5 w-3.5 text-primary" />
                New Password (min 8 characters)
              </label>
              <Input
                type="password"
                placeholder="••••••••••••"
                required
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                autoComplete="new-password"
                className="h-10 text-sm"
              />
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                <Lock className="h-3.5 w-3.5 text-primary" />
                Confirm New Password
              </label>
              <Input
                type="password"
                placeholder="••••••••••••"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                autoComplete="new-password"
                className="h-10 text-sm"
              />
            </div>

            <Button type="submit" variant="gradient" className="w-full font-medium" disabled={loading}>
              {loading ? "Updating Password..." : "Set New Password"}
            </Button>
          </form>
        )}

        {/* STEP 4: SUCCESS */}
        {step === "SUCCESS" && (
          <div className="space-y-4 py-2 text-center">
            <div className="h-12 w-12 rounded-full bg-emerald-500/10 text-emerald-500 flex items-center justify-center mx-auto">
              <CheckCircle2 className="h-7 w-7" />
            </div>
            <div className="space-y-1">
              <h4 className="font-bold text-base text-foreground">Password Successfully Updated!</h4>
              <p className="text-xs text-muted-foreground">
                All existing device sessions have been revoked. Please sign in with your new password.
              </p>
            </div>

            <Link href="/login" className="block pt-2">
              <Button variant="gradient" className="w-full font-medium">
                Proceed to Sign In
              </Button>
            </Link>
          </div>
        )}
      </CardContent>

      <CardFooter className="flex justify-center border-t border-border/40 pt-4 bg-muted/20">
        <Link href="/login" className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1.5 transition-colors">
          <ArrowLeft className="h-3.5 w-3.5" />
          Back to Sign In
        </Link>
      </CardFooter>
    </Card>
  );
}
