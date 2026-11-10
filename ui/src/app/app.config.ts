import {
    ApplicationConfig,
    provideAppInitializer,
    inject,
    provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import {
    provideHttpClient,
    withXsrfConfiguration,
    withInterceptors,
} from '@angular/common/http';
import { environment } from '../environments/environment';
import { firstValueFrom } from 'rxjs';
import { fr_FR, provideNzI18n } from 'ng-zorro-antd/i18n';
import { registerLocaleData } from '@angular/common';
import fr from '@angular/common/locales/fr';
import { provideNzDateFnsAdapter } from 'ng-zorro-antd/core/time';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { provideApi } from './api-auth';
import { unauthorizedInterceptor } from './interceptors/unauthorized.interceptor';
import { errorInterceptor } from './interceptors/error.interceptor';
import { languageInterceptor } from './interceptors/language.interceptor';
import { LanguageService } from './services/language.service';

registerLocaleData(fr);

export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideRouter(routes),
        provideHttpClient(
              withXsrfConfiguration({
                cookieName: 'XSRF-TOKEN',
                headerName: 'X-XSRF-TOKEN',
              }),
            withInterceptors([
                //     (req, next) => {
                //       const reqWithCreds = req.clone({
                //         withCredentials: true,
                //         url: `${environment.apiUrl}${req.url}`,
                //       });
                //       return next(reqWithCreds);
                //     },
                languageInterceptor,
                unauthorizedInterceptor,
                errorInterceptor,
            ]),
        ),
        provideTranslateService({ lang: 'fr', fallbackLang: 'fr' }),
        provideTranslateHttpLoader({ prefix: './i18n/', suffix: '.json' }),
        provideAppInitializer(() => firstValueFrom(inject(LanguageService).init())),
        provideNzI18n(fr_FR),
        provideNzDateFnsAdapter(),
        provideApi({
            basePath: environment.apiUrl,
        })
    ],
};
