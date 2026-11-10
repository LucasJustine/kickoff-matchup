import { Component, computed, DestroyRef, DOCUMENT, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { Lang, LANGS, LanguageService } from '../../services/language.service';
import { SessionService } from '../../services/session.service';

const DESKTOP_MIN_WIDTH = 768;

@Component({
    selector: 'app-header',
    templateUrl: './app-header.html',
    imports: [RouterLink, TranslatePipe, NzButtonModule, NzLayoutModule, NzMenuModule],
    host: { class: 'block' },
    styles: `
        dialog {
            transform: translateX(100%);
            transition:
                transform 300ms cubic-bezier(0.22, 1, 0.36, 1),
                overlay 300ms allow-discrete,
                display 300ms allow-discrete;
        }
        dialog[open] {
            transform: none;
            @starting-style {
                transform: translateX(100%);
            }
        }
        @media (prefers-reduced-motion: reduce) {
            dialog {
                transition: none;
            }
        }
    `,
})
export class AppHeader {
    private readonly root = inject(DOCUMENT).documentElement;
    private readonly router = inject(Router);
    private readonly session = inject(SessionService);
    private readonly language = inject(LanguageService);
    private readonly menu = viewChild.required<ElementRef<HTMLDialogElement>>('menu');

    protected readonly menuOpen = signal(false);
    protected readonly authenticated = this.session.authenticated;
    protected readonly langs = LANGS;
    protected readonly currentLang = this.language.current;

    protected readonly links = computed(() => [
        { path: '/welcome', label: 'app.nav.home' },
        this.authenticated()
            ? { path: '/profile', label: 'app.nav.profile' }
            : { path: '/auth/login', label: 'app.nav.login' },
    ]);

    constructor() {
        this.session.check().subscribe();

        effect(() => {
            const el = this.menu().nativeElement;
            const open = this.menuOpen();
            if (open && !el.open) el.showModal();
            else if (!open && el.open) el.close();
            this.root.classList.toggle('modal-open', open);
        });

        const desktop = matchMedia(`(min-width: ${DESKTOP_MIN_WIDTH}px)`);
        const onChange = (e: MediaQueryListEvent) => {
            if (e.matches) this.closeMenu();
        };
        desktop.addEventListener('change', onChange);

        inject(DestroyRef).onDestroy(() => {
            desktop.removeEventListener('change', onChange);
            this.root.classList.remove('modal-open');
        });
    }

    protected openMenu(): void {
        this.menuOpen.set(true);
    }

    protected closeMenu(): void {
        this.menuOpen.set(false);
    }

    protected onDialogClick(event: MouseEvent): void {
        if (event.target === this.menu().nativeElement) this.closeMenu();
    }

    protected setLanguage(lang: Lang): void {
        this.language.use(lang).subscribe();
    }

    protected logout(): void {
        this.closeMenu();
        this.session.logout().subscribe(() => this.router.navigate(['/auth/login']));
    }
}
