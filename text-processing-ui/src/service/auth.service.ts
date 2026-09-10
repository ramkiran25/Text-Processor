// auth.service.ts
import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  // Angular Signal tracking login state
  isAuthenticated = signal<boolean>(this.hasToken());
  currentUser = signal<string>(localStorage.getItem('currentUser') || 'Admin');

  private hasToken(): boolean {
    return localStorage.getItem('isAuthenticated') === 'true';
  }

  login(username: string): void {
    localStorage.setItem('isAuthenticated', 'true');
    localStorage.setItem('currentUser', username);

    this.isAuthenticated.set(true);
    this.currentUser.set(username);
  }

  logout(): void {
    localStorage.clear();
    this.isAuthenticated.set(false);
    this.currentUser.set('User');
  }
}
