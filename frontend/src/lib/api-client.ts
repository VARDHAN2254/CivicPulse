import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { authStorage } from "./auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = authStorage.getToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string; error?: string }>) => {
    if (error.response?.status === 401) {
      // Clear token if unauthorized
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        authStorage.removeToken();
      }
    } else if (error.response?.status === 429) {
      console.warn("CivicPulse Rate Limit Triggered:", error.response?.data?.message || "Too many requests. Please slow down.");
    }
    return Promise.reject(error);
  }
);
