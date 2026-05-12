import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { AuthService } from '../../../core/services/auth.service';
import { SessionService } from '../../../core/services/session.service';

@Component({
  selector: 'app-user-menu',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="user-menu">
      <button type="button" class="user-menu__trigger" (click)="open.update((value) => !value)">
        <span class="user-menu__avatar">{{ initials() }}</span>
        <span class="user-menu__identity">
          <strong>{{ displayName() }}</strong>
          <small>{{ roleSubtitle() }}</small>
        </span>
        <span class="user-menu__role">{{ session.role() }}</span>
      </button>

      @if (open()) {
        <div class="user-menu__panel card-shell">
          <div class="user-menu__meta">
            <strong>{{ displayName() }}</strong>
            <span>{{ session.user()?.email }}</span>
            <small>{{ roleLabel() }}</small>
          </div>
          <button type="button" (click)="auth.logout().subscribe()">Logout</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .user-menu { position: relative; }
    .user-menu__trigger {
      display: inline-flex;
      align-items: center;
      gap: 0.85rem;
      border: 1px solid var(--hc-border);
      background: var(--hc-elevated);
      color: var(--hc-text);
      padding: 0.5rem 0.75rem;
      border-radius: 999px;
    }
    .user-menu__identity {
      display: grid;
      gap: 0.1rem;
      text-align: left;
    }
    .user-menu__identity small,
    .user-menu__meta small {
      color: var(--hc-text-soft);
      text-transform: capitalize;
    }
    .user-menu__avatar {
      display: grid;
      place-items: center;
      width: 2.2rem;
      height: 2.2rem;
      border-radius: 50%;
      background: linear-gradient(135deg, #f59e0b 0%, #0ea5e9 100%);
      color: #0f172a;
      font-weight: 700;
    }
    .user-menu__role {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0.28rem 0.62rem;
      border-radius: 999px;
      background: rgba(14, 165, 233, 0.1);
      color: var(--hc-primary-strong);
      font-size: 0.74rem;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }
    .user-menu__panel {
      position: absolute;
      right: 0;
      top: calc(100% + 0.75rem);
      min-width: 12rem;
      padding: 0.6rem;
    }
    .user-menu__meta {
      display: grid;
      gap: 0.2rem;
      padding: 0.65rem 0.75rem 0.85rem;
      border-bottom: 1px solid var(--hc-border);
      margin-bottom: 0.45rem;
    }
    .user-menu__panel button {
      width: 100%;
      border: 0;
      background: transparent;
      color: inherit;
      text-align: left;
      padding: 0.7rem 0.75rem;
      border-radius: 0.85rem;
    }
    .user-menu__panel button:hover {
      background: rgba(14, 165, 233, 0.08);
    }
  `],
})
export class UserMenuComponent {
  protected readonly session = inject(SessionService);
  protected readonly auth = inject(AuthService);
  protected readonly open = signal(false);

  protected initials(): string {
    const name = this.displayName();
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase() || 'HC';
  }

  protected displayName(): string {
    const user = this.session.user();
    return user?.fullName || user?.email || 'HireConnect';
  }

  protected roleLabel(): string {
    switch (this.session.role()) {
      case 'RECRUITER':
        return 'Recruiter account';
      case 'ADMIN':
        return 'Admin account';
      case 'CANDIDATE':
      default:
        return 'Candidate account';
    }
  }

  protected roleSubtitle(): string {
    switch (this.session.role()) {
      case 'RECRUITER':
        return 'Hiring workspace';
      case 'ADMIN':
        return 'Platform operations';
      case 'CANDIDATE':
      default:
        return 'Career workspace';
    }
  }
}
