-- =============================================================================
-- Flyway Migration: V4__seed_initial_data.sql
-- Description: Seed initial categories, system administrator, demo roles & organizations
-- Passwords below are standard BCrypt hashes for: 'Password@2026!'
-- =============================================================================

-- 1. Default Categories
INSERT INTO categories (id, name, slug, description, icon_name, display_order) VALUES
('c0000000-0000-0000-0000-000000000001', 'Community & Civic', 'community-civic', 'Town halls, neighborhood cleanups, public forums, and local initiatives.', 'Users', 1),
('c0000000-0000-0000-0000-000000000002', 'Environment & Sustainability', 'environment-sustainability', 'Tree plantation drives, recycling workshops, and conservation programs.', 'Leaf', 2),
('c0000000-0000-0000-0000-000000000003', 'Tech & Innovation', 'tech-innovation', 'Hackathons, open-source sprints, AI workshops, and developer meetups.', 'Cpu', 3),
('c0000000-0000-0000-0000-000000000004', 'Arts & Culture', 'arts-culture', 'Exhibitions, music circles, theatrical plays, and cultural festivals.', 'Palette', 4),
('c0000000-0000-0000-0000-000000000005', 'Health & Wellness', 'health-wellness', 'Blood donation camps, mental health seminars, yoga, and 5K runs.', 'HeartPulse', 5),
('c0000000-0000-0000-0000-000000000006', 'Education & Skills', 'education-skills', 'Mentorship programs, coding bootcamps, career guidance, and literacy drives.', 'GraduationCap', 6),
('c0000000-0000-0000-0000-000000000007', 'Charity & Volunteering', 'charity-volunteering', 'Food distribution, fundraising galas, animal welfare, and NGO drives.', 'HandHeart', 7)
ON CONFLICT (slug) DO NOTHING;

-- 2. Default Event Tags
INSERT INTO event_tags (id, name, slug) VALUES
('d0000000-0000-0000-0000-000000000001', 'Volunteering', 'volunteering'),
('d0000000-0000-0000-0000-000000000002', 'Sustainability', 'sustainability'),
('d0000000-0000-0000-0000-000000000003', 'Free Entry', 'free-entry'),
('d0000000-0000-0000-0000-000000000004', 'Hands-on', 'hands-on'),
('d0000000-0000-0000-0000-000000000005', 'Family Friendly', 'family-friendly'),
('d0000000-0000-0000-0000-000000000006', 'Networking', 'networking'),
('d0000000-0000-0000-0000-000000000007', 'Certificate', 'certificate')
ON CONFLICT (slug) DO NOTHING;

-- 3. Default Seed Users
-- Password for all seed users: Password123!
-- BCrypt hash: $2a$12$5vtSeHcTaA5FA81.up.AUeUmWSepmcdq9F1peZK1otiXUfmKom0KW
INSERT INTO users (id, email, password_hash, full_name, role, is_active, is_email_verified) VALUES
('a0000000-0000-0000-0000-000000000001', 'admin@civicpulse.org', '$2a$12$5vtSeHcTaA5FA81.up.AUeUmWSepmcdq9F1peZK1otiXUfmKom0KW', 'Platform Administrator', 'ADMIN', TRUE, TRUE),
('a0000000-0000-0000-0000-000000000002', 'moderator@civicpulse.org', '$2a$12$5vtSeHcTaA5FA81.up.AUeUmWSepmcdq9F1peZK1otiXUfmKom0KW', 'Community Moderator', 'MODERATOR', TRUE, TRUE),
('a0000000-0000-0000-0000-000000000003', 'organizer@civicpulse.org', '$2a$12$5vtSeHcTaA5FA81.up.AUeUmWSepmcdq9F1peZK1otiXUfmKom0KW', 'Sarah Jenkins (Organizer)', 'ORGANIZER', TRUE, TRUE),
('a0000000-0000-0000-0000-000000000004', 'member@civicpulse.org', '$2a$12$5vtSeHcTaA5FA81.up.AUeUmWSepmcdq9F1peZK1otiXUfmKom0KW', 'Alex Rivera (Member)', 'MEMBER', TRUE, TRUE)
ON CONFLICT (email) DO NOTHING;

-- 4. Default Seed Organizations
INSERT INTO organizations (id, name, slug, description, website, contact_email, is_verified) VALUES
('b0000000-0000-0000-0000-000000000001', 'Green Earth Volunteers', 'green-earth-volunteers', 'Grassroots environmental protection, urban afforestation, and river rejuvenation drives.', 'https://greenearth.org', 'contact@greenearth.org', TRUE),
('b0000000-0000-0000-0000-000000000002', 'Metropolis Tech Council', 'metropolis-tech-council', 'Fostering civic technology, AI for good, and open government data initiatives.', 'https://metropolistech.org', 'info@metropolistech.org', TRUE)
ON CONFLICT (slug) DO NOTHING;

-- 5. Organization Memberships
INSERT INTO organization_memberships (id, organization_id, user_id, org_role) VALUES
('e0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003', 'OWNER'),
('e0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000003', 'ORGANIZER'),
('e0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', 'MEMBER')
ON CONFLICT (organization_id, user_id) DO NOTHING;
