import { Routes } from '@angular/router';
import { LoginForm } from './login-form/login-form';
import { TextProcessorComponent } from './text-processor/text-processor-component/text-processor-component';
import { authGuard } from './text-processor/auth.guard';

export const routes: Routes = [
	{ 
      path: 'login', 
      component: LoginForm 
    },
    { 
      path: 'processor', 
      component: TextProcessorComponent,
      canActivate: [authGuard] // 👈 Protect this route from unauthenticated traffic
    },
    { 
      path: '', 
      redirectTo: 'login', 
      pathMatch: 'full' 
    },
    { 
      path: '**', 
      redirectTo: 'login' // Optional catch-all fallback safety boundary redirect
    }
];
