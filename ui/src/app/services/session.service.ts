import { Service, inject, signal } from '@angular/core';
import { catchError, map, Observable, of, tap } from 'rxjs';
import { AuthControllerService, UserDto } from '../api-auth';

@Service()
export class SessionService {
    private readonly api = inject(AuthControllerService);

    readonly authenticated = signal(false);
    readonly user = signal<UserDto | null>(null);

    check(): Observable<boolean> {
        if (this.authenticated()) {
            return of(true);
        }

        return this.api.me().pipe(
            tap({
                next: (user) => {
                    this.authenticated.set(true);
                    this.user.set(user);
                },
            }),
            map(() => {
                return true;
            }),
            catchError((error) => {
                this.authenticated.set(false);
                this.user.set(null);
                return of(false);
            }),
        );
    }

    markAuthenticated(): void {
        this.authenticated.set(true);
    }

    clear(): void {
        this.authenticated.set(false);
        this.user.set(null);
    }
}
