export const DEFAULT_URL = '/';


export function sanitizeReturnUrl(raw: string | null | undefined): string | null {
    if (!raw || raw.length > 2048) return null;
    if (!raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\')) return null;
    if (/[\u0000-\u001f\u007f]/.test(raw)) return null;
    if (raw === '/auth/login' || raw.startsWith('/auth/login?') || raw.startsWith('/auth/login/')) return null;
    return raw;
}
