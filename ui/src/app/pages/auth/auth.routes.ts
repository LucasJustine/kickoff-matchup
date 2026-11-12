import { Routes } from '@angular/router';
import { guestGuard } from '../../guard/guest.guard';

export const authRoutes: Routes = [
    {
        path: 'login',
        canActivate: [guestGuard],
        loadComponent: () => import('./login/login').then((m) => m.Login),
    },
    {
        path: 'register',
        loadComponent: () => import('./register/register').then((m) => m.Register),
    },
  
    {
        'path': 'resend-verification',
        loadComponent: () => import('./email-request/email-request').then((m) => m.EmailRequest),
        data: { mode: 'resend' },
    }
];