import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { SessionService } from '../../services/session.service';

@Component({
    selector: 'app-welcome',
    imports: [RouterLink, TranslatePipe, NzButtonModule],
    templateUrl: './welcome.html',
    styleUrl: './welcome.css',
})
export class Welcome {
    private readonly session = inject(SessionService);

    protected readonly authenticated = this.session.authenticated;
    protected readonly user = this.session.user;
}
