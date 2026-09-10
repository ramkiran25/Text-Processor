import { Routes } from '@angular/router';
import { TextProcessorComponent } from '../text-processor-component/text-processor.component';
import { LoginComponent } from '../login-component/login/login';

export const routes: Routes = [
  { path: '', component: TextProcessorComponent, pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
];
