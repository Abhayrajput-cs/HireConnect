import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

type AppTheme = 'light' | 'dark';

const THEME_KEY = 'hireconnect.theme.v2';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly themeSignal = signal<AppTheme>('dark');

  readonly theme = computed(() => this.themeSignal());
  readonly isDark = computed(() => this.themeSignal() === 'dark');

  initialize(): void {
    const storage = this.getStorage();
    const storedTheme = storage?.getItem(THEME_KEY);

    if (storedTheme === 'light' || storedTheme === 'dark') {
      this.setTheme(storedTheme);
      return;
    }

    const prefersDark = typeof window !== 'undefined'
      && typeof window.matchMedia === 'function'
      && window.matchMedia('(prefers-color-scheme: dark)').matches;

    this.setTheme(prefersDark ? 'dark' : 'dark', false);
  }

  toggle(): void {
    this.setTheme(this.isDark() ? 'light' : 'dark');
  }

  setTheme(theme: AppTheme, persist = true): void {
    this.themeSignal.set(theme);
    this.document.documentElement.setAttribute('data-theme', theme);

    if (persist) {
      this.getStorage()?.setItem(THEME_KEY, theme);
    }
  }

  private getStorage(): Storage | null {
    return typeof window === 'undefined' ? null : window.localStorage;
  }
}
