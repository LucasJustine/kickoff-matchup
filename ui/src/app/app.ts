import { Component, effect, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { AppHeader } from './components/app-header/app-header';

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, NzLayoutModule, AppHeader],
    templateUrl: './app.html',
    styleUrl: './app.css',
})
export class App {
    private readonly title = inject(Title);
    private readonly translate = inject(TranslateService);

    constructor() {
        const pageTitle = this.translate.translate('app.title');
        effect(() => this.title.setTitle(String(pageTitle())));
    }
}
