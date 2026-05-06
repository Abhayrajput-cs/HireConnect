export type PaymentRole = 'CANDIDATE' | 'RECRUITER';

export type PlanType = 'CANDIDATE_FREE' | 'CANDIDATE_PREMIUM' | 'RECRUITER_FREE' | 'RECRUITER_PREMIUM';

export type PaymentStatus = 'CREATED' | 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

export type SubscriptionStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface SubscriptionPlanResponse {
  id: number;
  planType: PlanType;
  role: PaymentRole;
  displayName: string;
  amount: number;
  currency: string;
  durationDays: number;
  premium: boolean;
  benefits: string[];
}

export interface CreateOrderRequest {
  userId: number;
  role: PaymentRole;
  planType: PlanType;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
}

export interface CreateOrderResponse {
  orderId: string;
  gatewayOrderId: string | null;
  razorpayKeyId: string | null;
  amountInPaise: number | null;
  planType: PlanType;
  amount: number;
  currency: string;
  paymentStatus: PaymentStatus;
}

export interface VerifyPaymentRequest {
  orderId: string;
  transactionId?: string | null;
  razorpayOrderId?: string | null;
  razorpayPaymentId?: string | null;
  razorpaySignature?: string | null;
}

export interface PaymentTransactionResponse {
  id: number;
  orderId: string;
  transactionId: string | null;
  userId: number;
  role: PaymentRole;
  planType: PlanType;
  amount: number;
  currency: string;
  paymentStatus: PaymentStatus;
  startDate: string | null;
  expiryDate: string | null;
}

export interface UserSubscriptionResponse {
  subscriptionId: number;
  userId: number;
  role: PaymentRole;
  planType: PlanType;
  displayName: string;
  status: SubscriptionStatus;
  premium: boolean;
  startDate: string;
  expiryDate: string;
}

export interface SubscriptionStatusResponse {
  userId: number;
  premiumActive: boolean;
  planType: PlanType | null;
  status: SubscriptionStatus;
  expiryDate: string | null;
}
