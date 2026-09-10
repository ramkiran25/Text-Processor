import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { LoginRequest, LoginResponse } from '../../model/login.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  // State management using Signals
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  // Strictly typed Reactive Form
  loginForm = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(4)]],
  });

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    // Extract typed form values
    const loginRequest: LoginRequest = this.loginForm.getRawValue();

    // Call AuthService and subscribe safely
    this.authService.login(loginRequest).subscribe({
      next: (response: LoginResponse) => {
        this.isLoading.set(false);

        if (response.success) {
          localStorage.setItem('isAuthenticated', 'true');
          localStorage.setItem('currentUser', loginRequest.username);
          if (response.token) {
            localStorage.setItem('auth_token', response.token);
          }

          // Redirect to dashboard root route
          this.router.navigate(['/']);
        } else {
          this.errorMessage.set(response.message || 'Authentication failed.');
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err?.status === 401) {
          this.errorMessage.set('Invalid username or password.');
        } else {
          this.errorMessage.set('An error occurred during login. Please try again.');
        }
      },
    });
  }
}
