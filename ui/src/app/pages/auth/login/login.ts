import { Component, inject, signal } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { AuthControllerService, LoginResponse } from '../../../api-auth';
import { fieldError, handleApiError, toApiError } from '../../../api-auth/auth-shared';

@Component({
    selector: 'app-login',
    imports: [ReactiveFormsModule, RouterLink, NzAlertModule, NzButtonModule, NzFormModule, NzInputModule],
    templateUrl: './login.html',
})
export class Login {
    private readonly fb = inject(NonNullableFormBuilder);
    private readonly api = inject(AuthControllerService);
    private readonly router = inject(Router);

    protected readonly fieldError = fieldError;
    protected readonly loading = signal(false);
    protected readonly error = signal('');
    protected readonly emailNotVerified = signal(false);
    /** Renseigné quand l'API répond MFA_REQUIRED : on affiche alors l'étape « code ». */
    protected readonly challengeId = signal<string | null>(null);

    protected readonly form = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
        password: ['', Validators.required],
    });

    protected readonly mfaForm = this.fb.group({
        code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    });

    submit(): void {
        if (this.form.invalid) return this.form.markAllAsTouched();
        this.begin();
        this.api.login(this.form.getRawValue()).subscribe({
            next: (res: LoginResponse) => {
                this.loading.set(false);
                if (res.status === LoginResponse.StatusEnum.MfaRequired) {
                    this.challengeId.set(res.challengeId ?? null);
                } else {
                    this.success();
                }
            },
            error: (e) => this.fail(this.form, e),
        });
    }

    submitMfa(): void {
        if (this.mfaForm.invalid) return this.mfaForm.markAllAsTouched();
        this.begin();
        this.api
            .verifyMfa(
                { challengeId: this.challengeId()!, code: this.mfaForm.getRawValue().code }
            )
            .subscribe({
                next: () => this.success(),
                error: (e) => {
                    const code = toApiError(e).code;
                    // Challenge périmé ou trop d'essais : on repart de l'étape email/mot de passe.
                    if (code === 'MFA_CHALLENGE_EXPIRED' || code === 'MFA_TOO_MANY_ATTEMPTS') {
                        this.challengeId.set(null);
                        this.mfaForm.reset();
                    }
                    this.fail(this.mfaForm, e);
                },
            });
    }

    private begin(): void {
        this.loading.set(true);
        this.error.set('');
        this.emailNotVerified.set(false);
    }

    private fail(form: FormGroup, e: unknown): void {
        this.loading.set(false);
        this.emailNotVerified.set(toApiError(e).code === 'EMAIL_NOT_VERIFIED');
        this.error.set(handleApiError(form, e));
    }

    private success(): void {
        this.loading.set(false);
        // TODO: stocker res.tokens.accessToken (service de session) avant de naviguer.
        this.router.navigateByUrl('/welcome');
    }
}
