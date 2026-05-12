import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api.constants';
import { NotificationEvent, NotificationResponse } from '../models/notification.models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);

  publishEvent(payload: NotificationEvent): Observable<void> {
    return this.http.post<void>(`${API_ENDPOINTS.notifications}/events`, payload);
  }

  getByUser(userId: number, isRead?: boolean | null): Observable<NotificationResponse[]> {
    let params = new HttpParams();
    if (isRead !== null && isRead !== undefined) {
      params = params.set('isRead', String(isRead));
    }
    return this.http.get<NotificationResponse[]>(`${API_ENDPOINTS.notifications}/user/${userId}`, { params });
  }

  markAsRead(notificationId: number): Observable<void> {
    return this.http.patch<void>(`${API_ENDPOINTS.notifications}/${notificationId}/read`, {});
  }

  markAllRead(userId: number): Observable<void> {
    return this.http.patch<void>(`${API_ENDPOINTS.notifications}/user/${userId}/read-all`, {});
  }

  deleteNotification(notificationId: number): Observable<void> {
    return this.http.delete<void>(`${API_ENDPOINTS.notifications}/${notificationId}`);
  }

  getUnreadCount(userId: number): Observable<number> {
    return this.http.get<number>(`${API_ENDPOINTS.notifications}/user/${userId}/unread-count`);
  }

  downloadOfferLetter(applicationId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${API_ENDPOINTS.notifications}/offer-letters/${applicationId}`, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}
