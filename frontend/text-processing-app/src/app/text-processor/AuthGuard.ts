import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './authService';


export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true; // Grant access to the route
  } else {
    // Redirect unauthenticated requests back to login page
    return router.createUrlTree(['/login']);
  }
};