import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { SessionService } from '../services/session.service';


export const authGuard: CanActivateFn = (_route, state) => {
    const router = inject(Router);
    return inject(SessionService)
        .check()
        .pipe(map((ok) => (ok ? true : router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } }))));
};
