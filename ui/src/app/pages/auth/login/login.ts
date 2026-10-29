import { Component, inject, OnInit, signal } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { AuthControllerService, LoginResponse } from '../../../api-auth';
import { fieldError, handleApiError, toApiError } from '../../../api-auth/auth-shared';
import { ProvidersService } from '../../../services/providers.service';
import { DEFAULT_URL, sanitizeReturnUrl } from '../../../utils/sanitize';
import { SessionService } from '../../../services/session.service';
import { SocialLoginButton } from '../../../components/providers-button/social-login-button';

@Component({
    selector: 'app-login',
    imports: [ReactiveFormsModule, RouterLink, NzAlertModule, NzButtonModule, NzFormModule, NzInputModule, SocialLoginButton],
    templateUrl: './login.html',
})
export class Login implements OnInit {
    private readonly fb = inject(NonNullableFormBuilder);
    private readonly api = inject(AuthControllerService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly session = inject(SessionService);
    private readonly providersService = inject(ProvidersService);

    /** Page demandée avant d'être renvoyé ici (?returnUrl=/mon-espace), ou null. */
    private readonly returnUrl = sanitizeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));

    protected readonly providers = this.providersService.providers;
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

    ngOnInit(): void {
        this.providersService.loadProviders();

        // Erreur renvoyée par le gateway après un échec de connexion sociale (/login?error=CODE)
        const errorCode = this.route.snapshot.queryParamMap.get('error');
        if (errorCode) this.error.set(handleApiError(this.form, { code: errorCode }));
    }

    /** Redirection pleine page vers le gateway ; il ramène sur returnUrl après la connexion. */
    protected loginWithProvider(provider: { loginUrl: string }): void {
        const url = this.returnUrl
            ? `${provider.loginUrl}?returnUrl=${encodeURIComponent(this.returnUrl)}`
            : provider.loginUrl;
        window.location.assign(url);
    }

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
            .verifyMfa({ challengeId: this.challengeId()!, code: this.mfaForm.getRawValue().code })
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

    /** La session est posée par le gateway (cookie) : on retourne là où l'utilisateur voulait aller. */
    private success(): void {
        this.loading.set(false);
        this.session.markAuthenticated();
        this.router.navigateByUrl(this.returnUrl ?? DEFAULT_URL);
    }
}
