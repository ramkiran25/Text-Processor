import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../service/auth.service';
interface LoginForm {
  username: FormControl<string>;
  password: FormControl<string>;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly loginForm: FormGroup<LoginForm> = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  get username(): FormControl<string> {
    return this.loginForm.controls.username;
  }

  get password(): FormControl<string> {
    return this.loginForm.controls.password;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.authService.login('admin');
    this.router.navigate(['/']);
  }
}
