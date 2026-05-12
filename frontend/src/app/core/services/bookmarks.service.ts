import { Injectable } from '@angular/core';
import { signal } from '@angular/core';

const BOOKMARK_KEY = 'hireconnect.bookmarkedJobs';

@Injectable({ providedIn: 'root' })
export class BookmarksService {
  readonly bookmarkedJobIds = signal<number[]>(this.read());

  toggle(jobId: number): void {
    this.bookmarkedJobIds.update((ids) => {
      const next = ids.includes(jobId) ? ids.filter((id) => id !== jobId) : [...ids, jobId];
      this.persist(next);
      return next;
    });
  }

  isBookmarked(jobId: number): boolean {
    return this.bookmarkedJobIds().includes(jobId);
  }

  private read(): number[] {
    if (typeof window === 'undefined') {
      return [];
    }

    const raw = window.localStorage.getItem(BOOKMARK_KEY);
    if (!raw) {
      return [];
    }

    try {
      return JSON.parse(raw) as number[];
    } catch {
      return [];
    }
  }

  private persist(jobIds: number[]): void {
    if (typeof window === 'undefined') {
      return;
    }
    window.localStorage.setItem(BOOKMARK_KEY, JSON.stringify(jobIds));
  }
}
