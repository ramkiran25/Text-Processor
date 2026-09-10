import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive], // CommonModule removed as @if is built-in
  template: `
    @if (authService.isAuthenticated()) {
      <nav class="navbar">
        <div class="nav-brand">
          <span class="brand-title">Enterprise Text Processor</span>
        </div>

        <div class="nav-links">
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
            Text Engine
          </a>

          <button class="btn-logout" (click)="onLogout()">
            Logout ({{ authService.currentUser() }})
          </button>
        </div>
      </nav>
    }
  `,
  styles: [
    `
      .navbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        background-color: #ffffff;
        border-bottom: 1px solid #e2e8f0;
        padding: 0.75rem 2rem;
        color: #334155;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
        font-family:
          Inter,
          system-ui,
          -apple-system,
          sans-serif;
      }

      .brand-title {
        font-size: 1.1rem;
        font-weight: 600;
        color: #1e293b;
      }

      .nav-links {
        display: flex;
        align-items: center;
        gap: 1.25rem;
      }

      .nav-links a {
        color: #64748b;
        text-decoration: none;
        font-size: 0.9rem;
        font-weight: 500;
        padding: 0.45rem 0.85rem;
        border-radius: 6px;
        transition: all 0.2s ease;
      }

      .nav-links a:hover {
        color: #0f172a;
        background-color: #f1f5f9;
      }

      .nav-links a.active {
        color: #0f172a;
        background-color: #e2e8f0;
        font-weight: 600;
      }

      .btn-logout {
        background-color: transparent;
        color: #64748b;
        border: 1px solid #cbd5e1;
        padding: 0.45rem 0.85rem;
        font-size: 0.875rem;
        font-weight: 500;
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.2s ease;
      }

      .btn-logout:hover {
        background-color: #fef2f2;
        color: #dc2626;
        border-color: #fca5a5;
      }
    `,
  ],
})
export class NavbarComponent {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
