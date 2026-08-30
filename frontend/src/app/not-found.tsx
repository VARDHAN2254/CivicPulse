"use client";

import React from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Compass, ArrowLeft } from "lucide-react";

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] px-4 text-center">
      <div className="h-16 w-16 rounded-2xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mb-6">
        <Compass className="h-8 w-8 animate-spin-slow" />
      </div>
      <h1 className="text-4xl font-extrabold tracking-tight mb-2">404 — Page Not Found</h1>
      <p className="text-sm text-muted-foreground max-w-md mb-8">
        The community page, event drive, or resource you are looking for does not exist or has been moved.
      </p>
      <Link href="/">
        <Button variant="gradient" className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          Return to CivicPulse Home
        </Button>
      </Link>
    </div>
  );
}
