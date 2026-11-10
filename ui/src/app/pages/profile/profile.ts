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
import { NzTagModule } from 'ng-zorro-antd/tag';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
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

    // Double authentification
    protected readonly savingMfa = signal(false);
    protected readonly mfaModalOpen = signal(false);
    protected readonly mfaForm = this.fb.group({ password: ['', Validators.required] });

    // Mot de passe
    protected readonly savingPassword = signal(false);
    protected readonly passwordInfo = signal('');
    protected readonly passwordError = signal('');
    protected readonly passwordForm = this.fb.group({
        currentPassword: ['', Validators.required],
        newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
        confirm: ['', [Validators.required, sameAs('newPassword')]],
    });

    // Suppression du compte
    protected readonly deleteModalOpen = signal(false);
    protected readonly deleting = signal(false);
    protected readonly deleteForm = this.fb.group({ password: ['', Validators.required] });

    protected readonly loggingOut = signal(false);

    constructor() {
        // « confirm » dépend de « newPassword » : on le revalide quand celui-ci change.
        this.passwordForm.controls.newPassword.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.passwordForm.controls.confirm.updateValueAndValidity());
    }

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

    /** Activer : un clic. Désactiver : le mot de passe est demandé dans une modale. */
    protected toggleMfa(enable: boolean): void {
        if (!enable) {
            this.mfaForm.reset();
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
        this.api.setMfa({ enabled, currentPassword }).subscribe({
            next: (user) => {
                this.user.set(user);
                this.savingMfa.set(false);
                this.mfaModalOpen.set(false);
            },
            error: (e) => {
                this.savingMfa.set(false);
                if (this.mfaModalOpen()) {
                    // Le message s'affiche sous le champ mot de passe de la modale.
                    handleApiError(this.mfaForm, e);
                } else {
                    this.error.set(handleApiError(new FormGroup({}), e));
                }
            },
        });
    }

    protected changePassword(): void {
        if (this.passwordForm.invalid) return this.passwordForm.markAllAsTouched();
        this.passwordInfo.set('');
        this.passwordError.set('');
        this.savingPassword.set(true);
        // TODO: brancher l'endpoint « changer le mot de passe » quand l'auth-server l'exposera.
        // Envoyer { currentPassword, newPassword } (this.passwordForm.getRawValue()), puis :
        //   next  -> this.passwordForm.reset(); this.passwordInfo.set('profile.password.changed');
        //   error -> this.passwordError.set(handleApiError(this.passwordForm, e));
        this.savingPassword.set(false);
        this.passwordError.set('profile.password.unavailable');
    }

    protected openDeleteModal(): void {
        this.deleteForm.reset();
        this.deleteModalOpen.set(true);
    }

    protected cancelDelete(): void {
        this.deleteModalOpen.set(false);
    }

    protected deleteAccount(): void {
        if (this.deleteForm.invalid) return this.deleteForm.markAllAsTouched();
        // TODO: brancher l'endpoint « supprimer le compte » quand l'auth-server l'exposera.
        // Envoyer { password } (this.deleteForm.getRawValue()), puis this.session.clear() et
        // this.router.navigate(['/auth/login']).
        this.deleting.set(false);
        this.deleteModalOpen.set(false);
        this.error.set('profile.delete.unavailable');
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
