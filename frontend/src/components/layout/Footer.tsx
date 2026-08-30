import React from "react";
import Link from "next/link";
import { Compass, Heart, Shield, Github, Sparkles } from "lucide-react";

export function Footer() {
  return (
    <footer className="border-t border-border/40 bg-muted/30">
      <div className="container mx-auto px-4 sm:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand Col */}
          <div className="space-y-4 md:col-span-1">
            <div className="flex items-center gap-2.5">
              <div className="h-8 w-8 rounded-lg bg-gradient-to-tr from-emerald-600 to-teal-500 flex items-center justify-center text-white shadow-sm">
                <Compass className="h-4 w-4" />
              </div>
              <span className="font-bold text-base bg-clip-text text-transparent bg-gradient-to-r from-emerald-700 to-teal-800 dark:from-emerald-400 dark:to-teal-300">
                CivicPulse
              </span>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Empowering colleges, NGOs, community clubs, and volunteer groups with modern event coordination, waitlists, QR check-ins, and civic engagement.
            </p>
          </div>

          {/* Platform Links */}
          <div className="space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-foreground">
              Platform
            </h4>
            <ul className="space-y-2 text-xs text-muted-foreground">
              <li><Link href="/events" className="hover:text-primary transition-colors">Discover Events</Link></li>
              <li><Link href="/organizations" className="hover:text-primary transition-colors">Community Organizations</Link></li>
              <li><Link href="/events?type=VIRTUAL" className="hover:text-primary transition-colors">Virtual Webinars</Link></li>
              <li><Link href="/manage-events/create" className="hover:text-primary transition-colors">Host an Event</Link></li>
            </ul>
          </div>

          {/* Solutions */}
          <div className="space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-foreground">
              Solutions
            </h4>
            <ul className="space-y-2 text-xs text-muted-foreground">
              <li><span className="hover:text-primary transition-colors">University & College Clubs</span></li>
              <li><span className="hover:text-primary transition-colors">Environmental NGOs</span></li>
              <li><span className="hover:text-primary transition-colors">Civic Action Groups</span></li>
              <li><span className="hover:text-primary transition-colors">Professional Meetups</span></li>
            </ul>
          </div>

          {/* Architecture & Tech */}
          <div className="space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-foreground">
              Architecture
            </h4>
            <div className="space-y-2 text-xs text-muted-foreground">
              <div className="flex items-center gap-1.5 text-emerald-600 dark:text-emerald-400 font-medium">
                <Shield className="h-3.5 w-3.5" />
                <span>Zero Overselling Engine</span>
              </div>
              <p className="text-[11px] leading-relaxed">
                Powered by Java 21, Spring Boot 3, Next.js 14, PostgreSQL 16, Redis 7, and Kafka KRaft.
              </p>
            </div>
          </div>
        </div>

        <div className="mt-12 pt-6 border-t border-border/40 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-muted-foreground">
          <p>© 2026 Jai Sai Vardhan Reddy. All rights reserved.</p>
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1">
              Built for resilient community engagement <Heart className="h-3 w-3 text-red-500 fill-red-500 inline" />
            </span>
          </div>
        </div>
      </div>
    </footer>
  );
}
