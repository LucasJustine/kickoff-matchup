import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AbstractControl, FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

export interface ApiError {
    success?: false;
    code?: string;
    message?: string;
    fields?: Record<string, string>;
}

const KNOWN_CODES = new Set([
    'INVALID_CREDENTIALS',
    'EMAIL_NOT_VERIFIED',
    'INVALID_TOKEN',
    'MFA_INVALID_CODE',
    'MFA_CODE_EXPIRED',
    'MFA_TOO_MANY_ATTEMPTS',
    'MFA_RESEND_TOO_SOON',
    'MFA_TOO_MANY_RESENDS',
    'MFA_CHALLENGE_EXPIRED',
    'INVALID_REFRESH_TOKEN',
    'UNAUTHENTICATED',
    'UNSUPPORTED_PROVIDER',
    'INVALID_SOCIAL_TOKEN',
    'OAUTH_EMAIL_NOT_VERIFIED',
    'SOCIAL_PROVIDER_UNAVAILABLE',
    'INVALID_CURRENT_PASSWORD',
    'PASSWORD_REQUIRED_FOR_MFA',
    'VALIDATION_ERROR',
    'TOO_MANY_REQUESTS',
    'MAIL_QUEUE_UNAVAILABLE',
    'AUTH_SERVER_UNAVAILABLE',
    'AUTH_SERVER_ERROR',
    'NETWORK_ERROR',
]);

export function toApiError(err: unknown): ApiError {
    const body = err instanceof HttpErrorResponse ? err.error : err;
    return body && typeof body === 'object' ? (body as ApiError) : {};
}


export function handleApiError(form: FormGroup, err: unknown): string {
    const e = toApiError(err);
    let applied = false;
    for (const [name, msg] of Object.entries(e.fields ?? {})) {
        const control = form.get(name);
        if (control) {
            control.setErrors({ serverError: msg });
            control.markAsDirty();
            applied = true;
        }
    }
    if (applied) return '';
    if (e.code && KNOWN_CODES.has(e.code)) return `errors.${e.code}`;
    return e.message || 'errors.generic';
}

export function injectFieldError(): (c: AbstractControl | null) => string {
    const translate = inject(TranslateService);
    return (c) => {
        const e = c?.errors;
        if (!c || !e || !(c.dirty || c.touched)) return '';
        if (e['serverError']) return e['serverError'];
        if (e['required']) return translate.instant('validation.required');
        if (e['email']) return translate.instant('validation.email');
        if (e['minlength']) return translate.instant('validation.minlength', { count: e['minlength'].requiredLength });
        if (e['maxlength']) return translate.instant('validation.maxlength', { count: e['maxlength'].requiredLength });
        if (e['pattern']) return translate.instant('validation.pattern');
        if (e['mismatch']) return translate.instant('validation.mismatch');
        return translate.instant('validation.invalid');
    };
}

/** Validateur « doit être égal au champ `other` du même groupe ». */
export const sameAs = (other: string) => (c: AbstractControl) =>
    c.value === c.parent?.get(other)?.value ? null : { mismatch: true };
