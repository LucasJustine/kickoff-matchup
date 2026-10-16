import {
    ApplicationConfig,
    provideBrowserGlobalErrorListeners,
    provideAppInitializer,
    inject,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import {
    provideHttpClient,
    withXsrfConfiguration,
    withInterceptors,
    HttpErrorResponse,
    HttpInterceptorFn,
} from '@angular/common/http';
import { environment } from '../environments/environment';
import { forkJoin, catchError, throwError, switchMap, from } from 'rxjs';
import { fr_FR, provideNzI18n } from 'ng-zorro-antd/i18n';
import { registerLocaleData } from '@angular/common';
import fr from '@angular/common/locales/fr';
import { provideNzDateFnsAdapter } from 'ng-zorro-antd/core/time';
import { provideApi } from './api-auth';

registerLocaleData(fr);

export const errorInterceptor: HttpInterceptorFn = (req, next) =>
    next(req).pipe(
        catchError((err: HttpErrorResponse) => {
            if (err.status === 0) {
                return throwError(() => ({ success: false, message: 'Serveur injoignable'}));
            }

           

            return throwError(() => err);
        }),
    );

export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideRouter(routes),
        provideHttpClient(
            //   withXsrfConfiguration({
            //     cookieName: 'XSRF-TOKEN',
            //     headerName: 'X-XSRF-TOKEN',
            //   }),
            withInterceptors([
                //     (req, next) => {
                //       const reqWithCreds = req.clone({
                //         withCredentials: true,
                //         url: `${environment.apiUrl}${req.url}`,
                //       });
                //       return next(reqWithCreds);
                //     },
                errorInterceptor,
            ]),
        ),
        provideNzI18n(fr_FR),
        provideNzDateFnsAdapter(),
        provideApi({
            basePath: environment.apiUrl,
        })
    ],
};
