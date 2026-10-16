import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';

export interface ApiError {
    success?: false;
    code?: string;
    message?: string;
    fields?: Record<string, string>;
}

const MESSAGES: Record<string, string> = {
    INVALID_CREDENTIALS: 'Email ou mot de passe incorrect.',
    EMAIL_NOT_VERIFIED: "Votre email n'est pas encore vérifié.",
    INVALID_TOKEN: 'Ce lien est invalide, expiré ou déjà utilisé.',
    MFA_INVALID_CODE: 'Code incorrect.',
    MFA_CHALLENGE_EXPIRED: 'Code expiré. Reconnectez-vous.',
    MFA_TOO_MANY_ATTEMPTS: 'Trop de tentatives. Reconnectez-vous.',
    INVALID_REFRESH_TOKEN: 'Session expirée. Reconnectez-vous.',
    UNAUTHENTICATED: 'Session expirée. Reconnectez-vous.',
    UNSUPPORTED_PROVIDER: 'Fournisseur de connexion non supporté.',
    INVALID_SOCIAL_TOKEN: 'Connexion via le fournisseur refusée.',
    OAUTH_EMAIL_NOT_VERIFIED: "L'email de votre compte n'est pas vérifié chez le fournisseur.",
    SOCIAL_PROVIDER_UNAVAILABLE: 'Fournisseur indisponible, réessayez plus tard.',
};

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
    return (e.code && MESSAGES[e.code]) || e.message || 'Une erreur est survenue.';
}

/** Message d'erreur d'un champ, vide tant que le champ n'a pas été touché. */
export function fieldError(c: AbstractControl | null): string {
    const e = c?.errors;
    if (!c || !e || !(c.dirty || c.touched)) return '';
    if (e['serverError']) return e['serverError'];
    if (e['required']) return 'Champ obligatoire';
    if (e['email']) return 'Email invalide';
    if (e['minlength']) return `Minimum ${e['minlength'].requiredLength} caractères`;
    if (e['maxlength']) return `Maximum ${e['maxlength'].requiredLength} caractères`;
    if (e['pattern']) return 'Format invalide';
    if (e['mismatch']) return 'Les mots de passe ne correspondent pas';
    return 'Valeur invalide';
}

/** Validateur « doit être égal au champ `other` du même groupe ». */
export const sameAs = (other: string) => (c: AbstractControl) =>
    c.value === c.parent?.get(other)?.value ? null : { mismatch: true };
