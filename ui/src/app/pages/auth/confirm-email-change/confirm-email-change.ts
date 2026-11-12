import { Component, inject, OnInit, signal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { AuthControllerService } from '../../../api-auth';
import { handleApiError } from '../../../api-auth/auth-shared';
import { SessionService } from '../../../services/session.service';

@Component({
    selector: 'app-confirm-email-change',
    imports: [RouterLink, NzAlertModule, TranslatePipe],
    templateUrl: './confirm-email-change.html',
})
export class ConfirmEmailChange implements OnInit {
    private readonly api = inject(AuthControllerService);
    private readonly session = inject(SessionService);
    private readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');

    protected readonly authenticated = this.session.authenticated;
    protected readonly status = signal<'loading' | 'ok' | 'error'>('loading');
    protected readonly error = signal('');

    ngOnInit(): void {
        if (!this.token) {
            this.error.set('errors.invalidLink');
            this.status.set('error');
            return;
        }
        this.api.confirmEmailChange({ token: this.token }).subscribe({
            next: () => {
                this.status.set('ok');
                if (this.session.authenticated()) {
                    this.api.me().subscribe((user) => this.session.setUser(user));
                }
            },
            error: (e) => {
                this.error.set(handleApiError(new FormGroup({}), e));
                this.status.set('error');
            },
        });
    }
}
