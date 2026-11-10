import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) =>
    next(req).pipe(
        catchError((err: HttpErrorResponse) => {
            if (err.status === 0) {
                return throwError(() => ({ success: false, code: 'NETWORK_ERROR' }));
            }
            return throwError(() => err);
        }),
    );
