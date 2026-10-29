export const DEFAULT_URL = '/';


export function sanitizeReturnUrl(raw: string | null | undefined): string | null {
    if (!raw || raw.length > 2048) return null;
    if (!raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\')) return null;
    if (/[\u0000-\u001f\u007f]/.test(raw)) return null;
    if (raw === '/login' || raw.startsWith('/login?') || raw.startsWith('/login/')) return null;
    return raw;
}
