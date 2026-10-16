import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { AuthControllerService } from '../../../api-auth';
import { fieldError, handleApiError } from '../../../api-auth/auth-shared';

type Mode = 'forgot' | 'resend';

const TEXTS: Record<Mode, { title: string; hint: string; button: string; done: string }> = {
    forgot: {
        title: 'Mot de passe oublié',
        hint: 'Saisissez votre email pour recevoir un lien de réinitialisation.',
        button: 'Envoyer le lien',
        done: "Si un compte existe pour cette adresse, un email de réinitialisation vient d'être envoyé (lien valable 1 h).",
    },
    resend: {
        title: "Renvoyer l'email de confirmation",
        hint: "Saisissez l'email utilisé à l'inscription.",
        button: 'Renvoyer',
        done: "Si ce compte existe et n'est pas encore vérifié, un nouvel email vient d'être envoyé (lien valable 24 h).",
    },
};

@Component({
    selector: 'app-email-request',
    imports: [ReactiveFormsModule, RouterLink, NzAlertModule, NzButtonModule, NzFormModule, NzInputModule],
    templateUrl: './email-request.html',
})
export class EmailRequest {
    private readonly fb = inject(NonNullableFormBuilder);
    private readonly api = inject(AuthControllerService);
    private readonly mode = inject(ActivatedRoute).snapshot.data['mode'] as Mode;

    protected readonly texts = TEXTS[this.mode];
    protected readonly fieldError = fieldError;
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
