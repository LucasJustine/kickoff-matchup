

import { Component, inject, OnInit, signal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { finalize } from 'rxjs';
import { AuthControllerService, UserDto } from '../../api-auth';
import { handleApiError } from '../../api-auth/auth-shared';
import { SessionService } from '../../services/session.service';

@Component({
    selector: 'app-profile',
    imports: [NzAlertModule, NzButtonModule, NzDescriptionsModule],
    templateUrl: './profile.html',
})
export class Profile implements OnInit {
    private readonly api = inject(AuthControllerService);
    private readonly router = inject(Router);
    private readonly session = inject(SessionService);

    protected readonly user = signal<UserDto | null>(null);
    protected readonly loading = signal(true);
    protected readonly savingMfa = signal(false);
    protected readonly loggingOut = signal(false);
    protected readonly error = signal('');

    ngOnInit(): void {
        this.api.me().subscribe({
            next: (user: UserDto) => {
                this.user.set(user);
                this.loading.set(false);
            },
            error: (e: unknown) => {
                this.loading.set(false);
                this.error.set(handleApiError(new FormGroup({}), e));
            },
        });
    }

    protected toggleMfa(enabled: boolean): void {
        this.savingMfa.set(true);
        this.error.set('');
        this.api.setMfa({ enabled }).subscribe({
            next: (user) => {
                this.user.set(user);
                this.savingMfa.set(false);
            },
            error: (e) => {
                this.savingMfa.set(false);
                this.error.set(handleApiError(new FormGroup({}), e));
            },
        });
    }

    protected logout(): void {
        this.loggingOut.set(true);
        this.api
            .logout()
            .pipe(
                finalize(() => {
                    this.loggingOut.set(false);
                    this.session.clear();
                    this.router.navigate(['/auth/login']);
                }),
            )
            .subscribe({ error: () => undefined });
    }
}