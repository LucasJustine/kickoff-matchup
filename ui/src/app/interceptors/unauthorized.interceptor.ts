import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessionService } from '../services/session.service';


export const unauthorizedInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);
    const session = inject(SessionService);

    return next(req).pipe(
        catchError((err: HttpErrorResponse) => {
            if (err.status === 401 && !req.url.includes('/auth/')) {
                session.clear();
                router.navigate(['/auth/login'], { queryParams: { returnUrl: router.url } });
            }
            return throwError(() => err);
        }),
    );
};
