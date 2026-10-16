import { Component, inject, OnInit, signal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { AuthControllerService } from '../../../api-auth';
import { handleApiError } from '../../../api-auth/auth-shared';

@Component({
    selector: 'app-verify-email',
    imports: [RouterLink, NzAlertModule],
    templateUrl: './verify-email.html',
})
export class VerifyEmail implements OnInit {
    private readonly api = inject(AuthControllerService);
    /** Lien reçu par email : /verify-email?token=... */
    private readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');

    protected readonly status = signal<'loading' | 'ok' | 'error'>('loading');
    protected readonly error = signal('');

    ngOnInit(): void {
        if (!this.token) {
            this.error.set('Lien invalide.');
            this.status.set('error');
            return;
        }
        this.api.verifyEmail({ token: this.token }).subscribe({
            next: () => this.status.set('ok'),
            error: (e) => {
                this.error.set(handleApiError(new FormGroup({}), e));
                this.status.set('error');
            },
        });
    }
}
