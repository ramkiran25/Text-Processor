import { Routes } from '@angular/router';
import { LoginForm } from './login-form/login-form';
import { TextProcessorComponent } from './text-processor/text-processor-component/text-processor-component';

export const routes: Routes = [
	{ path: 'login', component: LoginForm },
	{ path: 'processor', component: TextProcessorComponent },
	{ path: '', redirectTo: 'login', pathMatch: 'full' }
];
