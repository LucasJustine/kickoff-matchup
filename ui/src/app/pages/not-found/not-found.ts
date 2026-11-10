import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { NzButtonModule } from 'ng-zorro-antd/button';

@Component({
    selector: 'app-not-found',
    imports: [RouterLink, TranslatePipe, NzButtonModule],
    template: `
        <div class="py-8">
            <h2>{{ 'notFound.title' | translate }}</h2>
            <p>{{ 'notFound.text' | translate }}</p>
            <a nz-button nzType="primary" routerLink="/welcome">{{ 'notFound.back' | translate }}</a>
        </div>
    `,
})
export class NotFound {}
