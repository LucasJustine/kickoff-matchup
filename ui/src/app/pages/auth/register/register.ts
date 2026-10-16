import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { AuthControllerService } from '../../../api-auth';
import { fieldError, handleApiError, sameAs } from '../../../api-auth/auth-shared';

@Component({
    selector: 'app-register',
    imports: [ReactiveFormsModule, RouterLink, NzAlertModule, NzButtonModule, NzFormModule, NzInputModule],
    templateUrl: './register.html',
})
export class Register {
    private readonly fb = inject(NonNullableFormBuilder);
    private readonly api = inject(AuthControllerService);

    protected readonly fieldError = fieldError;
    protected readonly loading = signal(false);
    protected readonly error = signal('');
    protected readonly sent = signal(false);

    protected readonly form = this.fb.group({
        firstName: ['', [Validators.required, Validators.maxLength(100)]],
        lastName: ['', [Validators.required, Validators.maxLength(100)]],
        email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
        password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
        confirm: ['', [Validators.required, sameAs('password')]],
    });

    submit(): void {
        this.form.controls.confirm.updateValueAndValidity(); // le mot de passe a pu changer depuis
        if (this.form.invalid) return this.form.markAllAsTouched();

        const { confirm, ...request } = this.form.getRawValue();
        this.loading.set(true);
        this.error.set('');
        this.api.register(request).subscribe({
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
