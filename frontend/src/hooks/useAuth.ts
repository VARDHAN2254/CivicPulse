"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { apiClient } from "@/lib/api-client";
import { authStorage } from "@/lib/auth";
import { User, AuthResponse, ApiResponse } from "@/types";

export interface AuthActionResult {
  success: boolean;
  message?: string;
  verificationRequired?: boolean;
  mfaRequired?: boolean;
  challengeId?: string;
  email?: string;
}

export function useAuth() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = authStorage.getUser();
    const token = authStorage.getToken();

    if (token && storedUser) {
      setUser(storedUser);
    }
    setLoading(false);
  }, []);

  const login = async (email: string, password: string): Promise<AuthActionResult> => {
    try {
      const response = await apiClient.post<ApiResponse<AuthResponse>>("/auth/login", {
        email: email.trim(),
        password,
      });

      if (response.data.success && response.data.data) {
        const data = response.data.data;

        // Step-Up MFA Challenge
        if (data.mfaRequired) {
          return {
            success: false,
            mfaRequired: true,
            challengeId: data.challengeId,
            email: data.email || email,
            message: data.message || "Multi-Factor Authentication required.",
          };
        }

        // Email Verification Required
        if (data.verificationRequired) {
          return {
            success: false,
            verificationRequired: true,
            challengeId: data.challengeId,
            email: data.email || email,
            message: data.message || "Email verification required.",
          };
        }

        if (data.accessToken && data.user) {
          authStorage.setToken(data.accessToken);
          authStorage.setUser(data.user);
          setUser(data.user);
          router.push("/dashboard");
          return { success: true };
        }
      }
      return { success: false, message: response.data.message || "Login failed" };
    } catch (err: any) {
      const msg = err.response?.data?.message || "Invalid email or password.";
      return { success: false, message: msg };
    }
  };

  const verifyMfaLogin = async (challengeId: string, email: string, otp: string): Promise<AuthActionResult> => {
    try {
      const response = await apiClient.post<ApiResponse<AuthResponse>>("/auth/mfa/verify-login", {
        challengeId,
        email: email.trim(),
        otp: otp.trim(),
        purpose: "LOGIN_MFA",
      });

      if (response.data.success && response.data.data?.accessToken && response.data.data?.user) {
        const { accessToken, user } = response.data.data;
        authStorage.setToken(accessToken);
        authStorage.setUser(user);
        setUser(user);
        router.push("/dashboard");
        return { success: true };
      }
      return { success: false, message: response.data.message || "Invalid MFA code." };
    } catch (err: any) {
      const msg = err.response?.data?.message || "Invalid or expired MFA code.";
      return { success: false, message: msg };
    }
  };

  const register = async (
    email: string,
    password: string,
    fullName: string,
    role: string,
    phoneNumber?: string
  ): Promise<AuthActionResult> => {
    try {
      const response = await apiClient.post<ApiResponse<AuthResponse>>("/auth/register", {
        email: email.trim(),
        password,
        fullName: fullName.trim(),
        role,
        phoneNumber,
      });

      if (response.data.success && response.data.data) {
        const data = response.data.data;
        if (data.verificationRequired) {
          return {
            success: true,
            verificationRequired: true,
            challengeId: data.challengeId,
            email: data.email || email,
            message: data.message || "Account registered. Please verify your email with the 8-digit code.",
          };
        }

        if (data.accessToken && data.user) {
          authStorage.setToken(data.accessToken);
          authStorage.setUser(data.user);
          setUser(data.user);
          router.push("/dashboard");
          return { success: true };
        }
      }
      return { success: false, message: response.data.message || "Registration failed" };
    } catch (err: any) {
      const msg = err.response?.data?.message || "Registration failed. Please check inputs.";
      return { success: false, message: msg };
    }
  };

  const verifyRegistration = async (challengeId: string, email: string, otp: string): Promise<AuthActionResult> => {
    try {
      const response = await apiClient.post<ApiResponse<AuthResponse>>("/auth/verify-registration", {
        challengeId,
        email: email.trim(),
        otp: otp.trim(),
        purpose: "REGISTRATION_VERIFICATION",
      });

      if (response.data.success && response.data.data?.accessToken && response.data.data?.user) {
        const { accessToken, user } = response.data.data;
        authStorage.setToken(accessToken);
        authStorage.setUser(user);
        setUser(user);
        router.push("/dashboard");
        return { success: true };
      }
      return { success: false, message: response.data.message || "Verification failed." };
    } catch (err: any) {
      const msg = err.response?.data?.message || "Invalid or expired verification code.";
      return { success: false, message: msg };
    }
  };

  const logout = () => {
    apiClient.post("/auth/logout").catch(() => {});
    authStorage.removeToken();
    setUser(null);
    router.push("/");
  };

  return {
    user,
    loading,
    isAuthenticated: !!user,
    login,
    verifyMfaLogin,
    register,
    verifyRegistration,
    logout,
  };
}
