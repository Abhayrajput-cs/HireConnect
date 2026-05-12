export interface ActivityItem {
  id: string;
  source: 'notification' | 'derived';
  notificationId?: number;
  type: string;
  title: string;
  message: string;
  status: string;
  createdAt: string;
  isRead: boolean;
  actionLabel?: string;
  actionLink?: string;
}
