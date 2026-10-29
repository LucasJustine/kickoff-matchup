import { Routes } from '@angular/router';
import { authGuard } from './guard/auth.guard';
import { EMAIL_LINK_ROUTES } from './pages/auth/email-link.routes';

export const routes: Routes = [
    { path: '', pathMatch: 'full', redirectTo: '/welcome' },
    {
        path: 'welcome',
        loadChildren: () => import('./pages/welcome/welcome.routes').then((m) => m.WELCOME_ROUTES),
    },
    {
        path: 'auth',
        loadChildren: () => import('./pages/auth/auth.routes').then((m) => m.authRoutes),
    },
    ...EMAIL_LINK_ROUTES,
    { path: 'profile', canActivate: [authGuard], loadComponent: () => import('./pages/profile/profile').then(m => m.Profile) },
];
