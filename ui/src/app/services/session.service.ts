import { HttpContext, HttpContextToken } from '@angular/common/http';
import { Service, inject, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, shareReplay, tap } from 'rxjs';
import { AuthControllerService, UserDto } from '../api-auth';


export const SKIP_AUTH_REDIRECT = new HttpContextToken<boolean>(() => false);

@Service()
export class SessionService {
    private readonly api = inject(AuthControllerService);

    readonly authenticated = signal(false);
    readonly user = signal<UserDto | null>(null);

    private pending?: Observable<boolean>;

    check(): Observable<boolean> {
        if (this.authenticated()) {
            return of(true);
        }

        this.pending ??= this.api
            .me('body', false, { context: new HttpContext().set(SKIP_AUTH_REDIRECT, true) })
            .pipe(
                map((user) => {
                    this.markAuthenticated(user);
                    return true;
                }),
                catchError(() => {
                    this.clear();
                    return of(false);
                }),
                finalize(() => (this.pending = undefined)),
                shareReplay(1),
            );
        return this.pending;
    }

    markAuthenticated(user?: UserDto | null): void {
        this.authenticated.set(true);
        if (user) this.user.set(user);
    }

    setUser(user: UserDto): void {
        this.user.set(user);
    }

    logout(): Observable<void> {
        return this.api.logout().pipe(
            map(() => undefined),
            catchError(() => of(undefined)),
            tap(() => this.clear()),
        );
    }

    clear(): void {
        this.authenticated.set(false);
        this.user.set(null);
    }
}
