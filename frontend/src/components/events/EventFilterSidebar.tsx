"use client";

import React from "react";
import { Category, LocationType } from "@/types";
import { Layers, MapPin, Globe, Filter, Sparkles } from "lucide-react";

interface EventFilterSidebarProps {
  categories: Category[];
  selectedCategory: string;
  onSelectCategory: (catSlug: string) => void;
  selectedLocationType: string;
  onSelectLocationType: (loc: string) => void;
  availableOnly: boolean;
  onToggleAvailableOnly: (avail: boolean) => void;
  onReset: () => void;
}

export function EventFilterSidebar({
  categories,
  selectedCategory,
  onSelectCategory,
  selectedLocationType,
  onSelectLocationType,
  availableOnly,
  onToggleAvailableOnly,
  onReset,
}: EventFilterSidebarProps) {
  return (
    <div className="space-y-6 p-5 rounded-2xl bg-card border border-border/80 h-fit">
      <div className="flex items-center justify-between pb-3 border-b">
        <h3 className="text-sm font-bold flex items-center gap-2">
          <Filter className="h-4 w-4 text-primary" />
          Filter Events
        </h3>
        <button
          type="button"
          onClick={onReset}
          className="text-xs text-primary hover:underline font-semibold"
        >
          Reset
        </button>
      </div>

      {/* Category Facet */}
      <div className="space-y-2">
        <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground block">
          Category
        </label>
        <div className="space-y-1">
          <button
            type="button"
            onClick={() => onSelectCategory("")}
            className={`w-full text-left px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              selectedCategory === ""
                ? "bg-primary/10 text-primary font-bold"
                : "text-muted-foreground hover:bg-muted"
            }`}
          >
            All Categories
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id || cat.slug}
              type="button"
              onClick={() => onSelectCategory(cat.slug)}
              className={`w-full text-left px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                selectedCategory === cat.slug
                  ? "bg-primary/10 text-primary font-bold"
                  : "text-muted-foreground hover:bg-muted"
              }`}
            >
              {cat.name}
            </button>
          ))}
        </div>
      </div>

      {/* Location Type Facet */}
      <div className="space-y-2 pt-2 border-t">
        <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground block">
          Format
        </label>
        <div className="grid grid-cols-3 gap-1.5">
          {[
            { label: "All", value: "" },
            { label: "In-Person", value: "IN_PERSON" },
            { label: "Virtual", value: "VIRTUAL" },
          ].map((loc) => (
            <button
              key={loc.label}
              type="button"
              onClick={() => onSelectLocationType(loc.value)}
              className={`px-2 py-1.5 rounded-md border text-[11px] font-semibold text-center transition-all ${
                selectedLocationType === loc.value
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-border text-muted-foreground hover:bg-muted"
              }`}
            >
              {loc.label}
            </button>
          ))}
        </div>
      </div>

      {/* Availability Toggle */}
      <div className="space-y-2 pt-2 border-t">
        <label className="text-xs font-semibold uppercase tracking-wider text-muted-foreground block">
          Availability
        </label>
        <div className="flex items-center">
          <input
            type="checkbox"
            id="availFilter"
            checked={availableOnly}
            onChange={(e) => onToggleAvailableOnly(e.target.checked)}
            className="h-4 w-4 rounded border-gray-300 text-emerald-600 focus:ring-emerald-500"
          />
          <label htmlFor="availFilter" className="ml-2 text-xs text-foreground font-medium">
            Open spots only (Hide full)
          </label>
        </div>
      </div>
    </div>
  );
}
