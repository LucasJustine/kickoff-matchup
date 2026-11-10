import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthControllerService, LoginResponse, UserDto } from '../../../api-auth';
import { handleApiError, injectFieldError, toApiError } from '../../../api-auth/auth-shared';
import { ProvidersService } from '../../../services/providers.service';
import { DEFAULT_URL, sanitizeReturnUrl } from '../../../utils/sanitize';
import { SessionService } from '../../../services/session.service';
import { SocialLoginButton } from '../../../components/providers-button/social-login-button';

@Component({
    selector: 'app-login',
    imports: [ReactiveFormsModule, RouterLink, NzAlertModule, NzButtonModule, NzFormModule, NzInputModule, SocialLoginButton, TranslatePipe],
    templateUrl: './login.html',
})
export class Login implements OnInit, OnDestroy {
    private readonly fb = inject(NonNullableFormBuilder);
    private readonly api = inject(AuthControllerService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly session = inject(SessionService);
    private readonly providersService = inject(ProvidersService);

    /** Page demandée avant d'être renvoyé ici (?returnUrl=/mon-espace), ou null. */
    private readonly returnUrl = sanitizeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));

    protected readonly providers = this.providersService.providers;
    protected readonly fieldError = injectFieldError();
    protected readonly loading = signal(false);
    protected readonly error = signal('');
    protected readonly emailNotVerified = signal(false);
    protected readonly challengeId = signal<string | null>(null);
    protected readonly resendIn = signal(0);
    protected readonly resendsExhausted = signal(false);
    protected readonly info = signal('');
    private timer?: ReturnType<typeof setInterval>;

    protected readonly form = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
        password: ['', Validators.required],
    });

    protected readonly mfaForm = this.fb.group({
        code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    });

    ngOnInit(): void {
        this.providersService.loadProviders();

        const errorCode = this.route.snapshot.queryParamMap.get('error');
        if (errorCode) this.error.set(handleApiError(this.form, { code: errorCode }));
        if (this.route.snapshot.queryParamMap.get('info') === 'password-changed') {
            this.info.set('auth.login.passwordChanged');
        }
    }

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
                    this.info.set('');
                    this.resendsExhausted.set(false);
                    this.startCountdown();
                } else {
                    this.success(res.user);
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
                next: (res) => this.success(res.user),
                error: (e) => {
                    const code = toApiError(e).code;
                    if (code === 'MFA_CHALLENGE_EXPIRED') {
                        this.leaveMfa();
                    }
                    this.fail(this.mfaForm, e);
                },
            });
    }

    resendMfa(): void {
        if (this.resendIn() > 0 || this.resendsExhausted() || !this.challengeId()) return;
        this.begin();
        this.api.resendMfa({ challengeId: this.challengeId()! }).subscribe({
            next: () => {
                this.loading.set(false);
                this.mfaForm.reset();
                this.info.set('auth.mfa.resent');
                this.startCountdown();
            },
            error: (e) => {
                const code = toApiError(e).code;
                if (code === 'MFA_CHALLENGE_EXPIRED') this.leaveMfa();
                if (code === 'MFA_TOO_MANY_RESENDS') this.resendsExhausted.set(true);
                if (code === 'MFA_RESEND_TOO_SOON') this.startCountdown();
                this.fail(this.mfaForm, e);
            },
        });
    }

    protected leaveMfa(): void {
        clearInterval(this.timer);
        this.resendIn.set(0);
        this.challengeId.set(null);
        this.mfaForm.reset();
        this.info.set('');
    }

    private startCountdown(seconds = 30): void {
        clearInterval(this.timer);
        this.resendIn.set(seconds);
        this.timer = setInterval(() => {
            this.resendIn.update((s) => Math.max(0, s - 1));
            if (this.resendIn() === 0) clearInterval(this.timer);
        }, 1000);
    }

    ngOnDestroy(): void {
        clearInterval(this.timer);
    }

    private begin(): void {
        this.loading.set(true);
        this.error.set('');
        this.info.set('');
        this.emailNotVerified.set(false);
    }

    private fail(form: FormGroup, e: unknown): void {
        this.loading.set(false);
        this.emailNotVerified.set(toApiError(e).code === 'EMAIL_NOT_VERIFIED');
        this.error.set(handleApiError(form, e));
    }

    /** La session est posée par le gateway (cookie) : on retourne là où l'utilisateur voulait aller. */
    private success(user?: UserDto): void {
        this.loading.set(false);
        this.session.markAuthenticated(user);
        this.router.navigateByUrl(this.returnUrl ?? DEFAULT_URL);
    }
}
