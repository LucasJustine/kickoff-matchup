import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessionService, SKIP_AUTH_REDIRECT } from '../services/session.service';

const ENTRY_PATHS = ['/auth/login', '/auth/mfa/verify', '/auth/mfa/resend', '/auth/logout'];

export const unauthorizedInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);
    const session = inject(SessionService);

    return next(req).pipe(
        catchError((err: HttpErrorResponse) => {
            const expected = req.context.get(SKIP_AUTH_REDIRECT) || ENTRY_PATHS.some((path) => req.url.endsWith(path));
            if (err.status === 401 && !expected) {
                session.clear();
                if (!router.url.startsWith('/auth/login')) {
                    router.navigate(['/auth/login'], { queryParams: { returnUrl: router.url } });
                }
            }
            return throwError(() => err);
        }),
    );
};
