import { Injectable, signal } from '@angular/core';

import { ToastMessage } from '../models/ui.models';

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly messages = signal<ToastMessage[]>([]);
  private nextId = 1;

  success(title: string, message: string): void {
    this.push('success', title, message);
  }

  error(title: string, message: string): void {
    this.push('error', title, message);
  }

  info(title: string, message: string): void {
    this.push('info', title, message);
  }

  dismiss(id: number): void {
    this.messages.update((items) => items.filter((item) => item.id !== id));
  }

  private push(tone: ToastMessage['tone'], title: string, message: string): void {
    const item: ToastMessage = { id: this.nextId++, title, message, tone };
    this.messages.update((items) => [...items, item]);
    setTimeout(() => this.dismiss(item.id), 4200);
  }
}
