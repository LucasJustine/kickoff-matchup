import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthControllerService } from '../../../api-auth';
import { handleApiError, injectFieldError } from '../../../api-auth/auth-shared';

type Mode = 'forgot' | 'resend';

/** Préfixe des clés de traduction (auth.<prefix>.title / hint / button / done) selon la route. */
const KEY_PREFIX: Record<Mode, string> = {
    forgot: 'auth.forgot',
    resend: 'auth.resendVerification',
};

@Component({
    selector: 'app-email-request',
    imports: [ReactiveFormsModule, RouterLink, NzAlertModule, NzButtonModule, NzFormModule, NzInputModule, TranslatePipe],
    templateUrl: './email-request.html',
})
export class EmailRequest {
    private readonly fb = inject(NonNullableFormBuilder);
    private readonly api = inject(AuthControllerService);
    private readonly mode = inject(ActivatedRoute).snapshot.data['mode'] as Mode;

    protected readonly keyPrefix = KEY_PREFIX[this.mode];
    protected readonly fieldError = injectFieldError();
    protected readonly loading = signal(false);
    protected readonly error = signal('');
    protected readonly sent = signal(false);

    protected readonly form = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
    });

    submit(): void {
        if (this.form.invalid) return this.form.markAllAsTouched();

        this.loading.set(true);
        this.error.set('');
        const request = this.form.getRawValue();
        const call = this.mode === 'forgot' ? this.api.forgotPassword(request) : this.api.resendVerification(request);
        call.subscribe({
            next: () => {
                this.loading.set(false);
                this.sent.set(true);
            },
            error: (e) => {
                this.loading.set(false);
                this.error.set(handleApiError(this.form, e));
            },
        });
    }
}
