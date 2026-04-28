export interface NavItem {
  label: string;
  route: string;
  icon: string;
  description?: string;
}

export interface ToastMessage {
  id: number;
  title: string;
  message: string;
  tone: 'success' | 'error' | 'info';
}
