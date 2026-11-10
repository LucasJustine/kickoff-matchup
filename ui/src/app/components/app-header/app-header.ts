import { Component, DestroyRef, DOCUMENT, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';

const DESKTOP_MIN_WIDTH = 768;

@Component({
    selector: 'app-header',
    templateUrl: './app-header.html',
    imports: [RouterLink, TranslatePipe, NzLayoutModule, NzMenuModule],
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
    private readonly menu = viewChild.required<ElementRef<HTMLDialogElement>>('menu');

    protected readonly menuOpen = signal(false);

    protected readonly links = [
        { path: '/welcome', label: 'app.nav.home' },
        { path: '/auth/login', label: 'app.nav.login' },
        { path: '/profile', label: 'app.nav.profile' },
    ];

    constructor() {
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
}
