export interface NotificationResponse {
  notificationId: number;
  userId: number;
  type: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationEvent {
  eventType?: string | null;
  notificationType?: string | null;
  message?: string | null;
  recipientUserIds?: number[] | null;
  recipientEmails?: string[] | null;
  broadcastRole?: string | null;
  emailSubject?: string | null;
  emailBody?: string | null;
  applicationId?: number | null;
  jobId?: number | null;
  recruiterId?: number | null;
  candidateId?: number | null;
  status?: string | null;
  appliedAt?: string | null;
  occurredAt?: string | null;
}

export interface JobViewRequest {
  viewerId?: number | null;
  occurredAt?: string | null;
}
