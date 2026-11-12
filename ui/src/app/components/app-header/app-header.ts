import { FormsModule } from '@angular/forms';
import { Component, computed, DestroyRef, DOCUMENT, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzDropdownModule } from 'ng-zorro-antd/dropdown';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzSegmentedModule } from 'ng-zorro-antd/segmented';
import { Lang, LANGS, LanguageService } from '../../services/language.service';
import { SessionService } from '../../services/session.service';

const DESKTOP_MIN_WIDTH = 768;

@Component({
    selector: 'app-header',
    templateUrl: './app-header.html',
    imports: [
        FormsModule,
        RouterLink,
        TranslatePipe,
        NzAvatarModule,
        NzDropdownModule,
        NzLayoutModule,
        NzMenuModule,
        NzSegmentedModule,
    ],
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
    protected readonly langOptions = LANGS.map((lang) => ({ value: lang, label: lang.toUpperCase() }));
    protected readonly currentLang = this.language.current;

    protected readonly firstName = computed(() => this.session.user()?.firstName ?? '');
    protected readonly fullName = computed(() => `${this.firstName()} ${this.session.user()?.lastName ?? ''}`.trim());
    protected readonly email = computed(() => this.session.user()?.email ?? '');
    protected readonly initials = computed(() => {
        const user = this.session.user();
        return `${user?.firstName?.[0] ?? ''}${user?.lastName?.[0] ?? ''}`.toUpperCase() || '?';
    });

    protected readonly navLinks = computed(() => [
        { path: '/welcome', label: 'app.nav.home' },
        ...(this.authenticated() ? [] : [{ path: '/auth/login', label: 'app.nav.login' }]),
    ]);

    protected readonly links = computed(() => [
        ...this.navLinks(),
        ...(this.authenticated() ? [{ path: '/profile', label: 'app.nav.profile' }] : []),
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

    protected setLanguage(lang: Lang | string | number): void {
        this.language.use(lang as Lang).subscribe();
    }

    protected logout(): void {
        this.closeMenu();
        this.session.logout().subscribe(() => this.router.navigate(['/auth/login']));
    }
}
