import { DOCUMENT, Service, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { en_US, fr_FR, NzI18nService } from 'ng-zorro-antd/i18n';
import { Observable } from 'rxjs';

export type Lang = 'fr' | 'en';

export const LANGS: readonly Lang[] = ['fr', 'en'];
const DEFAULT_LANG: Lang = 'fr';
const STORAGE_KEY = 'lang';

@Service()
export class LanguageService {
    private readonly translate = inject(TranslateService);
    private readonly nzI18n = inject(NzI18nService);
    private readonly root = inject(DOCUMENT).documentElement;

    readonly current = signal<Lang>(readStored());

    init(): Observable<unknown> {
        return this.use(this.current());
    }

    use(lang: Lang): Observable<unknown> {
        this.current.set(lang);
        this.root.lang = lang;
        this.nzI18n.setLocale(lang === 'en' ? en_US : fr_FR);
        
        try {
            localStorage.setItem(STORAGE_KEY, lang);
        } catch {
        }
        return this.translate.use(lang);
    }
}

function readStored(): Lang {
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        return LANGS.includes(stored as Lang) ? (stored as Lang) : DEFAULT_LANG;
    } catch {
        return DEFAULT_LANG;
    }
}
