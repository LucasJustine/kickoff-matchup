import { Routes } from '@angular/router';
import { EmailRequest } from './email-request/email-request';

export const loginRoutes: Routes = [
    {
        path: 'login',
        loadComponent: () => import('./login/login').then((m) => m.Login),
    },
    {
        path: 'register',
        loadComponent: () => import('./register/register').then((m) => m.Register),
    },
    {
        'path': 'forgot-password',
        loadComponent: () => import('./email-request/email-request').then((m) => m.EmailRequest),
        data: { mode: 'forgot' },
    },
    {
        'path': 'resend-verification',
        loadComponent: () => import('./email-request/email-request').then((m) => m.EmailRequest),
        data: { mode: 'resend' },
    },
    {
        'path': 'reset-password',
        loadComponent: () => import('./reset-password/reset-password').then((m) => m.ResetPassword),
    }
];