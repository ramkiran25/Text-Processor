import { Routes } from '@angular/router';
import { TextProcessorComponent } from '../text-processor-component/text-processor.component';
import { LoginComponent } from '../login-component/login/login';
import { authGuard } from '../guard/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: TextProcessorComponent,
    canActivate: [authGuard],
  },
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
