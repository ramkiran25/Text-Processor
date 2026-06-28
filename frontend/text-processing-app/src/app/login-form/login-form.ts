import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router,RouterModule } from '@angular/router'

@Component({
    selector: 'app-login-form',
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './login-form.html',
    styleUrl: './login-form.css',
})
export class LoginForm {
    constructor(private router: Router) {}
    onSubmit() {
		this.router.navigate(['/processor']);
    }
    loginForm = new FormGroup({
        email: new FormControl('', [Validators.required, Validators.email]),
        password: new FormControl('', Validators.required),
    });

    login() {
        // CALL API with username and password
        if (this.loginForm.invalid) return;

        alert('Calling backend to login');
    }
}
