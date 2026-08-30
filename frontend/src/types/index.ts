export type UserRole = 'MEMBER' | 'ORGANIZER' | 'MODERATOR' | 'ADMIN';
export type OrgRole = 'OWNER' | 'ORGANIZER' | 'MEMBER';
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'REGISTRATION_OPEN' | 'REGISTRATION_CLOSED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type RegistrationStatus = 'CONFIRMED' | 'CANCELLED' | 'ATTENDED';
export type WaitlistStatus = 'PENDING' | 'PROMOTED' | 'EXPIRED' | 'CANCELLED';
export type LocationType = 'IN_PERSON' | 'VIRTUAL' | 'HYBRID';
export type NotificationType = 'REG_CONFIRM' | 'WAITLIST_PROMO' | 'REMINDER' | 'ANNOUNCEMENT' | 'SYSTEM';
export type ReportStatus = 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED' | 'DISMISSED';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  phoneNumber?: string;
  avatarUrl?: string;
  bio?: string;
  isActive?: boolean;
  active?: boolean;
  isEmailVerified?: boolean;
  emailVerified?: boolean;
  createdAt: string;
}

export type UserDto = User;

export interface AuthResponse {
  accessToken?: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  user?: User;
  verificationRequired?: boolean;
  mfaRequired?: boolean;
  challengeId?: string;
  email?: string;
  message?: string;
}

export interface OtpChallengeResponse {
  challengeId: string;
  email: string;
  purpose: 'REGISTRATION_VERIFICATION' | 'PASSWORD_RESET' | 'LOGIN_MFA' | 'EMAIL_CHANGE' | 'SENSITIVE_ACTION';
  expiresInSeconds: number;
  cooldownSeconds: number;
  resendsRemaining: number;
  mfaRequired?: boolean;
  verificationRequired?: boolean;
}

export interface VerifyOtpResponse {
  verified: boolean;
  resetAuthToken?: string;
  message?: string;
}

export interface Organization {
  id: string;
  name: string;
  slug: string;
  description?: string;
  logoUrl?: string;
  website?: string;
  contactEmail?: string;
  isVerified: boolean;
  createdAt: string;
}

export interface OrgMemberDto {
  id: string;
  userId: string;
  email: string;
  fullName: string;
  avatarUrl?: string;
  role: OrgRole;
  joinedAt: string;
}

export interface RegistrationDto {
  id: string;
  ticketCode: string;
  eventId: string;
  eventTitle: string;
  eventSlug?: string;
  startTime: string;
  endTime: string;
  venueName?: string;
  organizationName: string;
  userId: string;
  userEmail: string;
  userFullName: string;
  status: "CONFIRMED" | "CANCELLED" | "ATTENDED";
  registeredAt: string;
  cancelledAt?: string;
}

export interface WaitlistEntryDto {
  id: string;
  eventId: string;
  eventTitle: string;
  eventSlug?: string;
  startTime: string;
  organizationName: string;
  userId: string;
  userEmail: string;
  userFullName: string;
  position: number;
  status: "PENDING" | "PROMOTED" | "EXPIRED" | "CANCELLED";
  joinedAt: string;
  promotedAt?: string;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  description?: string;
  iconName?: string;
  displayOrder: number;
}

export interface EventTag {
  id: string;
  name: string;
  slug: string;
}

export interface EventItem {
  id: string;
  organizationId: string;
  organizationName?: string;
  organizationSlug?: string;
  organizationLogo?: string;
  createdByUserId: string;
  categoryId: string;
  categoryName?: string;
  categorySlug?: string;
  title: string;
  slug: string;
  description: string;
  shortDescription?: string;
  status: EventStatus;
  visibility: 'PUBLIC' | 'UNLISTED' | 'PRIVATE';
  startTime: string;
  endTime: string;
  registrationDeadline: string;
  capacity: number;
  currentRegistrationCount: number;
  locationType: LocationType;
  venueName?: string;
  address?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  latitude?: number;
  longitude?: number;
  virtualMeetingUrl?: string;
  bannerImageUrl?: string;
  waitlistEnabled: boolean;
  waitlistCapacity: number;
  tags?: EventTag[];
  createdAt: string;
  updatedAt: string;
}

export interface EventRegistration {
  id: string;
  eventId: string;
  userId: string;
  status: RegistrationStatus;
  ticketCode: string;
  registeredAt: string;
  cancelledAt?: string;
  event?: EventItem;
}

export interface AttendanceRecord {
  id: string;
  eventId: string;
  userId: string;
  registrationId: string;
  checkInMethod: 'QR_SCAN' | 'MANUAL_OVERRIDE';
  checkedInAt: string;
  eventTitle?: string;
  userFullName?: string;
}

export interface NotificationItem {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: NotificationType;
  actionUrl?: string;
  isRead: boolean;
  readAt?: string;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isLast: boolean;
  isFirst: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}
