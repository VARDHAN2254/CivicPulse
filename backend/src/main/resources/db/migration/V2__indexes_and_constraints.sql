-- =============================================================================
-- Flyway Migration: V2__indexes_and_constraints.sql
-- Description: Uniqueness constraints, check constraints, and performance indexes
-- =============================================================================

-- Partial unique constraint: A user can have only ONE active (CONFIRMED) registration per event
CREATE UNIQUE INDEX uq_active_event_registration 
ON event_registrations (event_id, user_id) 
WHERE status = 'CONFIRMED';

-- Partial unique constraint: A user can have only ONE active (PENDING) waitlist entry per event
CREATE UNIQUE INDEX uq_active_event_waitlist 
ON waitlist_entries (event_id, user_id) 
WHERE status = 'PENDING';

-- Capacity & time integrity constraints on events
ALTER TABLE events 
ADD CONSTRAINT chk_event_capacity CHECK (capacity > 0),
ADD CONSTRAINT chk_event_reg_count CHECK (current_registration_count >= 0 AND current_registration_count <= capacity),
ADD CONSTRAINT chk_event_times CHECK (start_time < end_time),
ADD CONSTRAINT chk_event_reg_deadline CHECK (registration_deadline <= start_time);

-- Indexes on Events for discovery & filtering
CREATE INDEX idx_events_start_status ON events (start_time, status) WHERE status IN ('PUBLISHED', 'REGISTRATION_OPEN');
CREATE INDEX idx_events_org_id ON events (organization_id);
CREATE INDEX idx_events_category_id ON events (category_id);
CREATE INDEX idx_events_created_by ON events (created_by_user_id);
CREATE INDEX idx_events_city ON events (city);

-- Indexes on Registrations
CREATE INDEX idx_registrations_user ON event_registrations (user_id, status);
CREATE INDEX idx_registrations_event ON event_registrations (event_id, status);
CREATE INDEX idx_registrations_ticket ON event_registrations (ticket_code);

-- Indexes on Waitlist
CREATE INDEX idx_waitlist_fifo ON waitlist_entries (event_id, position) WHERE status = 'PENDING';
CREATE INDEX idx_waitlist_user ON waitlist_entries (user_id);

-- Indexes on Attendance
CREATE INDEX idx_attendance_event ON attendance_records (event_id);
CREATE INDEX idx_attendance_user ON attendance_records (user_id);

-- Indexes on Notifications
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read, created_at DESC);

-- Indexes on Discussions
CREATE INDEX idx_posts_event ON community_posts (event_id, created_at DESC) WHERE status = 'ACTIVE';
CREATE INDEX idx_comments_post ON comments (post_id, created_at ASC) WHERE status = 'ACTIVE';

-- Indexes on Reports & Moderation
CREATE INDEX idx_reports_status ON reports (status, created_at DESC);
CREATE INDEX idx_reports_target ON reports (target_type, target_id);

-- Indexes on Audit Logs
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_user_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id, created_at DESC);
