import React from "react";
import Link from "next/link";
import { Compass, Sparkles, ShieldCheck, HeartHandshake } from "lucide-react";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-[calc(100vh-4rem)] flex flex-col md:flex-row items-stretch">
      {/* Left side: Brand Showcase */}
      <div className="hidden md:flex md:w-1/2 bg-gradient-to-tr from-emerald-950 via-slate-900 to-teal-950 p-12 flex-col justify-between text-white relative overflow-hidden">
        <div className="relative z-10">
          <Link href="/" className="inline-flex items-center gap-2.5 group mb-8">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-500 flex items-center justify-center text-white shadow-lg group-hover:scale-105 transition-transform">
              <Compass className="h-6 w-6" />
            </div>
            <span className="font-bold text-xl tracking-tight text-white">CivicPulse</span>
          </Link>

          <div className="space-y-4 max-w-md mt-16">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-semibold">
              <Sparkles className="h-3.5 w-3.5" />
              <span>Community First Architecture</span>
            </div>
            <h2 className="text-3xl font-extrabold tracking-tight leading-tight">
              Connect with purpose. Empower local action.
            </h2>
            <p className="text-sm text-slate-300 leading-relaxed">
              Join thousands of community members, event coordinators, and civic groups driving measurable social impact.
            </p>
          </div>
        </div>

        <div className="relative z-10 grid grid-cols-2 gap-4 pt-12 border-t border-white/10 text-xs text-slate-300">
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-emerald-400" />
            <span>Guaranteed Zero Overselling</span>
          </div>
          <div className="flex items-center gap-2">
            <HeartHandshake className="h-4 w-4 text-emerald-400" />
            <span>Verified NGO Hubs</span>
          </div>
        </div>

        {/* Ambient background blur */}
        <div className="absolute top-1/3 left-1/4 w-[400px] h-[400px] bg-emerald-500/10 blur-[120px] rounded-full pointer-events-none -z-0" />
      </div>

      {/* Right side: Auth Card Container */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-12 bg-background">
        <div className="w-full max-w-md space-y-6">{children}</div>
      </div>
    </div>
  );
}
