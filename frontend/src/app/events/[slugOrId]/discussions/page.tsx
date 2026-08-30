"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { apiClient } from "@/lib/api-client";
import { authStorage } from "@/lib/auth";
import { formatDate } from "@/lib/utils";
import { 
  MessageSquare, 
  Pin, 
  CornerDownRight, 
  Send, 
  Flag, 
  Trash2, 
  ArrowLeft, 
  Sparkles,
  AlertCircle,
  CheckCircle2,
  ShieldAlert,
  User as UserIcon
} from "lucide-react";

interface DiscussionPost {
  id: string;
  eventId: string;
  parentPostId?: string;
  userId: string;
  userFullName: string;
  userEmail: string;
  userRole: string;
  content: string;
  pinned: boolean;
  status: string;
  replies: DiscussionPost[];
  createdAt: string;
}

export default function EventDiscussionsPage() {
  const params = useParams();
  const slugOrId = params.slugOrId as string;

  const [posts, setPosts] = useState<DiscussionPost[]>([]);
  const [newContent, setNewContent] = useState("");
  const [replyingToId, setReplyingToId] = useState<string | null>(null);
  const [replyContent, setReplyContent] = useState("");
  const [reportingPostId, setReportingPostId] = useState<string | null>(null);
  const [reportReason, setReportReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const currentUser = authStorage.getUser();

  const fetchPosts = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get<any>(`/events/${slugOrId}/discussions`);
      if (response.data.success && response.data.data) {
        setPosts(response.data.data.content || []);
      }
    } catch {
      // Fallback demo posts
      setPosts([
        {
          id: "p1",
          eventId: "e1",
          userId: "u1",
          userFullName: "Sarah Jenkins",
          userEmail: "organizer@civicpulse.org",
          userRole: "ORGANIZER",
          content: "Welcome everyone! Feel free to ask questions about tool assignments, parking, or carpooling coordinates here.",
          pinned: true,
          status: "ACTIVE",
          replies: [
            {
              id: "r1",
              eventId: "e1",
              parentPostId: "p1",
              userId: "u2",
              userFullName: "Alex Rivera",
              userEmail: "alex@example.org",
              userRole: "MEMBER",
              content: "Thanks Sarah! Will there be gloves provided or should we bring our own heavy-duty ones?",
              pinned: false,
              status: "ACTIVE",
              replies: [],
              createdAt: "2026-08-21T10:30:00Z"
            },
            {
              id: "r2",
              eventId: "e1",
              parentPostId: "p1",
              userId: "u1",
              userFullName: "Sarah Jenkins",
              userEmail: "organizer@civicpulse.org",
              userRole: "ORGANIZER",
              content: "We have over 100 pairs of heavy-duty gardening gloves ready at Pavilion #2, but feel free to bring your own favorite pair!",
              pinned: false,
              status: "ACTIVE",
              replies: [],
              createdAt: "2026-08-21T11:00:00Z"
            }
          ],
          createdAt: "2026-08-20T08:00:00Z"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPosts();
  }, [slugOrId]);

  const handleCreateRootPost = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newContent.trim()) return;

    setSubmitting(true);
    setNotice(null);

    try {
      const response = await apiClient.post<any>(`/events/${slugOrId}/discussions`, {
        content: newContent.trim(),
      });

      if (response.data.success) {
        setNewContent("");
        if (response.data.data?.status === "FLAGGED") {
          setNotice("Your post has been submitted and flagged for moderator review due to community safety filters.");
        } else {
          setNotice("Comment posted successfully!");
        }
        fetchPosts();
      }
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to post comment.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateReply = async (parentPostId: string) => {
    if (!replyContent.trim()) return;

    try {
      const response = await apiClient.post<any>(`/events/${slugOrId}/discussions`, {
        parentPostId,
        content: replyContent.trim(),
      });

      if (response.data.success) {
        setReplyingToId(null);
        setReplyContent("");
        fetchPosts();
      }
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to submit reply.");
    }
  };

  const handleTogglePin = async (postId: string) => {
    try {
      await apiClient.patch(`/events/${slugOrId}/discussions/${postId}/pin`);
      fetchPosts();
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to pin post.");
    }
  };

  const handleDeletePost = async (postId: string) => {
    if (!confirm("Are you sure you want to delete this comment?")) return;
    try {
      await apiClient.delete(`/events/${slugOrId}/discussions/${postId}`);
      fetchPosts();
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to delete post.");
    }
  };

  const handleReportPost = async (postId: string) => {
    if (!reportReason.trim()) return;

    try {
      await apiClient.post(`/events/${slugOrId}/discussions/${postId}/report`, {
        reason: reportReason.trim(),
      });
      setReportingPostId(null);
      setReportReason("");
      alert("Thank you. Report has been submitted to moderators.");
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to report post.");
    }
  };

  return (
    <div className="container mx-auto px-4 sm:px-8 py-10 space-y-8 max-w-4xl">
      <Link href={`/events/${slugOrId}`} className="inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground">
        <ArrowLeft className="h-3.5 w-3.5" />
        Back to event details
      </Link>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/40 pb-6">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">Community Discussion Board</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Collaborate, ask questions to coordinators, coordinate logistics, and connect with volunteers.
          </p>
        </div>
      </div>

      {notice && (
        <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>{notice}</span>
        </div>
      )}

      {/* New Post Box */}
      <Card className="border-border bg-card shadow-sm">
        <form onSubmit={handleCreateRootPost}>
          <CardContent className="p-5 space-y-3">
            <label className="text-xs font-bold text-foreground flex items-center gap-1.5">
              <MessageSquare className="h-3.5 w-3.5 text-primary" />
              Join the Conversation
            </label>
            <textarea
              rows={3}
              required
              placeholder="Ask a question, offer a carpool ride, or share helpful tips..."
              value={newContent}
              onChange={(e) => setNewContent(e.target.value)}
              className="flex w-full rounded-xl border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
            <div className="flex items-center justify-between pt-1">
              <span className="text-[11px] text-muted-foreground">
                Be kind and respectful. Automated profanity and safety checks are active.
              </span>
              <Button type="submit" variant="gradient" size="sm" disabled={submitting} className="gap-1.5 text-xs font-semibold">
                <Send className="h-3.5 w-3.5" />
                {submitting ? "Posting..." : "Post Comment"}
              </Button>
            </div>
          </CardContent>
        </form>
      </Card>

      {/* Thread List */}
      <div className="space-y-4">
        {loading ? (
          <div className="space-y-4">
            {[1, 2].map((i) => (
              <div key={i} className="h-32 rounded-2xl bg-muted/40 animate-pulse border" />
            ))}
          </div>
        ) : posts.length === 0 ? (
          <div className="p-16 text-center border rounded-2xl bg-card space-y-3">
            <MessageSquare className="h-10 w-10 text-muted-foreground mx-auto" />
            <h3 className="text-base font-bold">No discussion comments yet</h3>
            <p className="text-xs text-muted-foreground">Be the first to start the conversation for this community event!</p>
          </div>
        ) : (
          posts.map((post) => (
            <Card
              key={post.id}
              className={`border transition-all ${
                post.pinned ? "border-emerald-500/40 bg-emerald-500/5" : "border-border bg-card"
              }`}
            >
              <CardContent className="p-5 space-y-4">
                {/* Author Ribbon */}
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="h-9 w-9 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-xs">
                      {post.userFullName ? post.userFullName.charAt(0).toUpperCase() : "U"}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-foreground">{post.userFullName}</span>
                        {['ORGANIZER', 'ADMIN'].includes(post.userRole) && (
                          <Badge variant="success" className="text-[10px] py-0">Organizer</Badge>
                        )}
                        {post.pinned && (
                          <Badge variant="outline" className="gap-1 text-[10px] py-0 text-emerald-600 dark:text-emerald-400 border-emerald-500/40">
                            <Pin className="h-2.5 w-2.5" />
                            Pinned
                          </Badge>
                        )}
                      </div>
                      <span className="text-[11px] text-muted-foreground">{formatDate(post.createdAt)}</span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-1 text-muted-foreground">
                    {['ORGANIZER', 'ADMIN'].includes(currentUser?.role || '') && (
                      <button
                        onClick={() => handleTogglePin(post.id)}
                        className="h-7 w-7 rounded-lg hover:bg-muted hover:text-foreground flex items-center justify-center"
                        title={post.pinned ? "Unpin post" : "Pin post to top"}
                      >
                        <Pin className="h-3.5 w-3.5" />
                      </button>
                    )}

                    <button
                      onClick={() => setReportingPostId(post.id)}
                      className="h-7 w-7 rounded-lg hover:bg-muted hover:text-destructive flex items-center justify-center"
                      title="Report content"
                    >
                      <Flag className="h-3.5 w-3.5" />
                    </button>

                    {(currentUser?.id === post.userId || ['ORGANIZER', 'ADMIN'].includes(currentUser?.role || '')) && (
                      <button
                        onClick={() => handleDeletePost(post.id)}
                        className="h-7 w-7 rounded-lg hover:bg-muted hover:text-destructive flex items-center justify-center"
                        title="Delete post"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    )}
                  </div>
                </div>

                {/* Content */}
                <p className="text-sm text-foreground/90 leading-relaxed whitespace-pre-line">
                  {post.content}
                </p>

                {/* Reply Trigger */}
                <div className="pt-2 flex items-center justify-between border-t border-border/40">
                  <button
                    onClick={() => setReplyingToId(replyingToId === post.id ? null : post.id)}
                    className="text-xs text-primary font-semibold hover:underline flex items-center gap-1.5"
                  >
                    <CornerDownRight className="h-3.5 w-3.5" />
                    {replyingToId === post.id ? "Cancel Reply" : `Reply (${post.replies?.length || 0})`}
                  </button>
                </div>

                {/* Reply Box */}
                {replyingToId === post.id && (
                  <div className="pt-2 pl-4 border-l-2 border-primary/40 space-y-2">
                    <textarea
                      rows={2}
                      placeholder={`Reply to ${post.userFullName}...`}
                      value={replyContent}
                      onChange={(e) => setReplyContent(e.target.value)}
                      className="flex w-full rounded-lg border border-input bg-background px-3 py-2 text-xs ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    />
                    <div className="flex justify-end gap-2">
                      <Button size="sm" variant="ghost" className="text-xs h-7" onClick={() => setReplyingToId(null)}>
                        Cancel
                      </Button>
                      <Button size="sm" variant="gradient" className="text-xs h-7" onClick={() => handleCreateReply(post.id)}>
                        Submit Reply
                      </Button>
                    </div>
                  </div>
                )}

                {/* Nested Replies Tree */}
                {post.replies && post.replies.length > 0 && (
                  <div className="pl-4 sm:pl-6 border-l-2 border-border/60 space-y-3 pt-2">
                    {post.replies.map((reply) => (
                      <div key={reply.id} className="p-3 rounded-xl bg-muted/30 border border-border/30 space-y-1.5">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-xs text-foreground">{reply.userFullName}</span>
                            {['ORGANIZER', 'ADMIN'].includes(reply.userRole) && (
                              <Badge variant="success" className="text-[9px] px-1 py-0">Organizer</Badge>
                            )}
                            <span className="text-[10px] text-muted-foreground">{formatDate(reply.createdAt)}</span>
                          </div>

                          <div className="flex items-center gap-1">
                            <button
                              onClick={() => setReportingPostId(reply.id)}
                              className="h-5 w-5 rounded hover:bg-muted text-muted-foreground hover:text-destructive flex items-center justify-center"
                              title="Report reply"
                            >
                              <Flag className="h-3 w-3" />
                            </button>
                            {(currentUser?.id === reply.userId || ['ORGANIZER', 'ADMIN'].includes(currentUser?.role || '')) && (
                              <button
                                onClick={() => handleDeletePost(reply.id)}
                                className="h-5 w-5 rounded hover:bg-muted text-muted-foreground hover:text-destructive flex items-center justify-center"
                                title="Delete reply"
                              >
                                <Trash2 className="h-3 w-3" />
                              </button>
                            )}
                          </div>
                        </div>

                        <p className="text-xs text-foreground/90 leading-relaxed whitespace-pre-line">
                          {reply.content}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* Report Modal */}
      {reportingPostId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-sm rounded-2xl bg-card border border-border p-6 space-y-4 shadow-2xl">
            <h3 className="font-bold text-base flex items-center gap-2">
              <ShieldAlert className="h-5 w-5 text-destructive" />
              Report Post to Moderation
            </h3>
            <p className="text-xs text-muted-foreground">
              Please specify why this comment violates CivicPulse community rules (harassment, spam, scam, hate speech).
            </p>
            <textarea
              rows={3}
              required
              placeholder="Explain the issue..."
              value={reportReason}
              onChange={(e) => setReportReason(e.target.value)}
              className="flex w-full rounded-lg border border-input bg-background px-3 py-2 text-xs"
            />
            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button variant="ghost" size="sm" onClick={() => setReportingPostId(null)}>
                Cancel
              </Button>
              <Button variant="destructive" size="sm" onClick={() => handleReportPost(reportingPostId)}>
                Submit Report
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
