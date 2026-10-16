import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { AuthControllerService } from '../../../api-auth';
import { fieldError, handleApiError, sameAs } from '../../../api-auth/auth-shared';

@Component({
    selector: 'app-reset-password',
    imports: [ReactiveFormsModule, RouterLink, NzAlertModule, NzButtonModule, NzFormModule, NzInputModule],
    templateUrl: './reset-password.html',
})
export class ResetPassword {
    private readonly fb = inject(NonNullableFormBuilder);
    private readonly api = inject(AuthControllerService);
    /** Lien reçu par email : /reset-password?token=... */
    protected readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');

    protected readonly fieldError = fieldError;
    protected readonly loading = signal(false);
    protected readonly error = signal('');
    protected readonly done = signal(false);

    protected readonly form = this.fb.group({
        newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
        confirm: ['', [Validators.required, sameAs('newPassword')]],
    });

    submit(): void {
        this.form.controls.confirm.updateValueAndValidity();
        if (this.form.invalid || !this.token) return this.form.markAllAsTouched();

        this.loading.set(true);
        this.error.set('');
        this.api.resetPassword({ token: this.token, newPassword: this.form.getRawValue().newPassword }).subscribe({
            next: () => {
                this.loading.set(false);
                this.done.set(true);
            },
            error: (e) => {
                this.loading.set(false);
                this.error.set(handleApiError(this.form, e));
            },
        });
    }
}
