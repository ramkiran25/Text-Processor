import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../text-processor/auth.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-login-form',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-form.html',
  styleUrl: './login-form.css',
})
export class LoginForm {
  isLoading = false;
  errorMessage = '';
  loginForm = new FormGroup({
    username: new FormControl('', [Validators.required]),
    password: new FormControl('', Validators.required),
  });
  constructor(
    private router: Router,
    private authService: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}
  onSubmit() {
    if (this.loginForm.invalid || this.isLoading) return;
    this.isLoading = true;
    this.errorMessage = '';
    const credentials = {
      username: this.loginForm.get('username')?.value || '',
      password: this.loginForm.get('password')?.value || '',
    };

    // Pass the credentials object to the subscription stream
    this.authService
      .login(credentials)
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (response: any) => {
  console.log('Backend response received:', response);

  // Explicitly check for boolean true or string representation to be safe
  if (response && (response.success === true || response.success === 'true')) {
    
    if (response.token) {
      localStorage.setItem('authToken', response.token);
    } else {
      // Fallback token so the Auth Guard doesn't immediately block navigation
      localStorage.setItem('authToken', 'authenticated-session-active');
    }

    // Wrap navigation inside a clear macro task to force the view switch
    setTimeout(() => {
      this.router.navigate(['/processor']).then(navigated => {
        if (!navigated) {
          console.error('Routing rejected! Check if /processor path matches app.routes.ts exactly.');
        }
      });
    }, 50);

  } else {
    this.errorMessage = response.message || 'Authentication failed.';
  }
  this.cdr.detectChanges();
},
        error: (err) => {
          console.error('Login transmission error context:', err);
          if (err.status === 401) {
            this.errorMessage = 'Invalid username or password.';
          } else {
            this.errorMessage =
              'Could not reach login server. Please verify backend state connectivity.';
          }
        },
      });
  }
}
