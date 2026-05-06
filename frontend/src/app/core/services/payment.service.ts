import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';

import { API_ENDPOINTS } from '../constants/api.constants';
import {
  ApiResponse,
  CreateOrderRequest,
  CreateOrderResponse,
  PaymentRole,
  PaymentTransactionResponse,
  SubscriptionPlanResponse,
  SubscriptionStatusResponse,
  UserSubscriptionResponse,
  VerifyPaymentRequest,
} from '../models/payment.models';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);

  getPlans(role: PaymentRole): Observable<SubscriptionPlanResponse[]> {
    return this.http.get<ApiResponse<SubscriptionPlanResponse[]>>(`${API_ENDPOINTS.payments}/plans/${role}`).pipe(map((response) => response.data));
  }

  createOrder(payload: CreateOrderRequest): Observable<CreateOrderResponse> {
    return this.http.post<ApiResponse<CreateOrderResponse>>(`${API_ENDPOINTS.payments}/create-order`, payload).pipe(map((response) => response.data));
  }

  verify(payload: VerifyPaymentRequest): Observable<PaymentTransactionResponse> {
    return this.http.post<ApiResponse<PaymentTransactionResponse>>(`${API_ENDPOINTS.payments}/verify`, payload).pipe(map((response) => response.data));
  }

  getSubscription(userId: number): Observable<UserSubscriptionResponse> {
    return this.http.get<ApiResponse<UserSubscriptionResponse>>(`${API_ENDPOINTS.payments}/subscription/${userId}`).pipe(map((response) => response.data));
  }

  getStatus(userId: number): Observable<SubscriptionStatusResponse> {
    return this.http.get<ApiResponse<SubscriptionStatusResponse>>(`${API_ENDPOINTS.payments}/subscription/status/${userId}`).pipe(map((response) => response.data));
  }

  cancel(userId: number): Observable<UserSubscriptionResponse> {
    return this.http.post<ApiResponse<UserSubscriptionResponse>>(`${API_ENDPOINTS.payments}/cancel/${userId}`, {}).pipe(map((response) => response.data));
  }
}
