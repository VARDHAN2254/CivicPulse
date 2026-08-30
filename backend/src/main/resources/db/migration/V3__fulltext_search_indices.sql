-- =============================================================================
-- Flyway Migration: V3__fulltext_search_indices.sql
-- Description: Full-text search (tsvector) and Trigram fuzzy search setup
-- =============================================================================

-- Enable pg_trgm and unaccent extensions
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Add search vector generated column to events table
ALTER TABLE events 
ADD COLUMN search_vector tsvector 
GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(short_description, '')), 'B') ||
    setweight(to_tsvector('english', coalesce(description, '')), 'C') ||
    setweight(to_tsvector('english', coalesce(venue_name, '') || ' ' || coalesce(city, '')), 'D')
) STORED;

-- Create GIN index for full-text search
CREATE INDEX idx_events_fulltext_search ON events USING GIN (search_vector);

-- Create Trigram GIN indexes for fuzzy search & typo tolerance
CREATE INDEX idx_events_title_trgm ON events USING GIN (title gin_trgm_ops);
CREATE INDEX idx_orgs_name_trgm ON organizations USING GIN (name gin_trgm_ops);
