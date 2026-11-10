import { Routes } from '@angular/router';

export const EMAIL_LINK_ROUTES: Routes = [
    {
        path: 'verify-email',
        loadComponent: () => import('./verify-email/verify-email').then((m) => m.VerifyEmail),
    },
    {
        path: 'confirm-email-change',
        loadComponent: () => import('./confirm-email-change/confirm-email-change').then((m) => m.ConfirmEmailChange),
    },
    {
        path: 'reset-password',
        loadComponent: () => import('./reset-password/reset-password').then((m) => m.ResetPassword),
    },
    {
        'path': 'forgot-password',
        loadComponent: () => import('./email-request/email-request').then((m) => m.EmailRequest),
        data: { mode: 'forgot' },
    }
];