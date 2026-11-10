import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthControllerService, UserDto } from '../../api-auth';
import { handleApiError, injectFieldError, sameAs } from '../../api-auth/auth-shared';
import { SessionService } from '../../services/session.service';

@Component({
    selector: 'app-profile',
    imports: [
        ReactiveFormsModule,
        NzAlertModule,
        NzAvatarModule,
        NzButtonModule,
        NzCardModule,
        NzFormModule,
        NzInputModule,
        NzModalModule,
        NzTabsModule,
        NzTagModule,
        TranslatePipe,
    ],
    templateUrl: './profile.html',
})
export class Profile implements OnInit {
    private readonly fb = inject(NonNullableFormBuilder);
    private readonly api = inject(AuthControllerService);
    private readonly router = inject(Router);
    private readonly session = inject(SessionService);
    private readonly destroyRef = inject(DestroyRef);

    protected readonly fieldError = injectFieldError();

    protected readonly user = signal<UserDto | null>(null);
    protected readonly loading = signal(true);
    protected readonly error = signal('');
    protected readonly initials = computed(() => {
        const u = this.user();
        return `${u?.firstName?.[0] ?? ''}${u?.lastName?.[0] ?? ''}`.toUpperCase() || '?';
    });

    protected readonly savingIdentity = signal(false);
    protected readonly identityInfo = signal('');
    protected readonly identityError = signal('');
    protected readonly emailModalOpen = signal(false);
    protected readonly emailModalError = signal('');
    protected readonly savingEmail = signal(false);
    protected readonly emailInfo = signal('');
    protected readonly pendingEmail = signal('');
    protected readonly emailForm = this.fb.group({
        newEmail: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
        password: ['', Validators.required],
    });
    protected readonly identityForm = this.fb.group({
        firstName: ['', [Validators.required, Validators.maxLength(100)]],
        lastName: ['', [Validators.required, Validators.maxLength(100)]],
    });

    protected readonly savingMfa = signal(false);
    protected readonly mfaModalOpen = signal(false);
    protected readonly mfaModalError = signal('');
    protected readonly mfaForm = this.fb.group({ password: ['', Validators.required] });

    protected readonly savingPassword = signal(false);
    protected readonly passwordError = signal('');
    protected readonly passwordForm = this.fb.group({
        currentPassword: ['', Validators.required],
        newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
        confirm: ['', [Validators.required, sameAs('newPassword')]],
    });

    protected readonly deleteModalOpen = signal(false);
    protected readonly deleting = signal(false);
    protected readonly deleteError = signal('');
    protected readonly deleteForm = this.fb.group({ password: ['', Validators.required] });

    protected readonly loggingOut = signal(false);

    constructor() {
        this.passwordForm.controls.newPassword.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.passwordForm.controls.confirm.updateValueAndValidity());
    }

    ngOnInit(): void {
        this.api.me().subscribe({
            next: (user: UserDto) => {
                this.setUser(user);
                this.loading.set(false);
            },
            error: (e: unknown) => {
                this.loading.set(false);
                this.error.set(handleApiError(new FormGroup({}), e));
            },
        });
    }

    protected saveIdentity(): void {
        if (this.identityForm.invalid) return this.identityForm.markAllAsTouched();
        this.savingIdentity.set(true);
        this.identityInfo.set('');
        this.identityError.set('');
        this.api.updateProfile(this.identityForm.getRawValue()).subscribe({
            next: (user) => {
                this.setUser(user);
                this.savingIdentity.set(false);
                this.identityInfo.set('profile.identity.saved');
            },
            error: (e) => {
                this.savingIdentity.set(false);
                this.identityError.set(handleApiError(this.identityForm, e));
            },
        });
    }

    protected toggleMfa(enable: boolean): void {
        if (!enable) {
            this.mfaForm.reset();
            this.mfaModalError.set('');
            this.mfaModalOpen.set(true);
            return;
        }
        this.updateMfa(true);
    }

    protected confirmDisableMfa(): void {
        if (this.mfaForm.invalid) return this.mfaForm.markAllAsTouched();
        this.updateMfa(false, this.mfaForm.getRawValue().password);
    }

    protected cancelDisableMfa(): void {
        this.mfaModalOpen.set(false);
    }

    private updateMfa(enabled: boolean, currentPassword?: string): void {
        this.savingMfa.set(true);
        this.error.set('');
        this.mfaModalError.set('');
        this.api.setMfa({ enabled, currentPassword }).subscribe({
            next: (user) => {
                this.setUser(user);
                this.savingMfa.set(false);
                this.mfaModalOpen.set(false);
            },
            error: (e) => {
                this.savingMfa.set(false);
                if (this.mfaModalOpen()) {
                    this.mfaModalError.set(handleApiError(this.mfaForm, e));
                } else {
                    this.error.set(handleApiError(new FormGroup({}), e));
                }
            },
        });
    }

    protected changePassword(): void {
        if (this.passwordForm.invalid) return this.passwordForm.markAllAsTouched();
        this.passwordError.set('');
        this.savingPassword.set(true);
        const { currentPassword, newPassword } = this.passwordForm.getRawValue();
        this.api.changePassword({ currentPassword, newPassword }).subscribe({
            next: () => this.leave(['/auth/login'], { info: 'password-changed' }),
            error: (e) => {
                this.savingPassword.set(false);
                this.passwordError.set(handleApiError(this.passwordForm, e));
            },
        });
    }

    protected openEmailModal(): void {
        this.emailForm.reset();
        this.emailModalError.set('');
        this.emailModalOpen.set(true);
    }

    protected cancelEmailModal(): void {
        this.emailModalOpen.set(false);
    }

    protected submitEmailChange(): void {
        if (this.emailForm.invalid) return this.emailForm.markAllAsTouched();
        const { newEmail, password } = this.emailForm.getRawValue();
        this.savingEmail.set(true);
        this.emailModalError.set('');
        this.api.changeEmail({ newEmail, currentPassword: password }).subscribe({
            next: () => {
                this.savingEmail.set(false);
                this.emailModalOpen.set(false);
                this.pendingEmail.set(newEmail.trim());
                this.emailInfo.set('profile.email.sent');
            },
            error: (e) => {
                this.savingEmail.set(false);
                this.emailModalError.set(handleApiError(this.emailForm, e));
            },
        });
    }

    protected openDeleteModal(): void {
        this.deleteForm.reset();
        this.deleteError.set('');
        this.deleteModalOpen.set(true);
    }

    protected cancelDelete(): void {
        this.deleteModalOpen.set(false);
    }

    protected deleteAccount(): void {
        if (this.deleteForm.invalid) return this.deleteForm.markAllAsTouched();
        this.deleting.set(true);
        this.deleteError.set('');
        this.api.deleteAccount({ currentPassword: this.deleteForm.getRawValue().password }).subscribe({
            next: () => this.leave(['/welcome']),
            error: (e) => {
                this.deleting.set(false);
                this.deleteError.set(handleApiError(this.deleteForm, e));
            },
        });
    }

    protected logout(): void {
        this.loggingOut.set(true);
        this.leave(['/auth/login']);
    }

    private leave(commands: string[], queryParams?: Record<string, string>): void {
        this.session.logout().subscribe(() => this.router.navigate(commands, { queryParams }));
    }

    private setUser(user: UserDto): void {
        this.user.set(user);
        this.session.setUser(user);
        this.identityForm.reset({ firstName: user.firstName ?? '', lastName: user.lastName ?? '' });
    }
}
