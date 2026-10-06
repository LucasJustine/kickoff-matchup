import { Component, inject, OnInit } from '@angular/core';
import { ProviderService } from '../../services/provider.service';

@Component({
  selector: 'app-login',
  standalone: true,
  template: `
    <div class="login-container">
      <h2>Connexion</h2>

      <div class="provider-buttons">
        @for (provider of providerService.providers(); track provider.id) {
          <button (click)="providerService.login(provider.url, '/')">
            Se connecter avec {{ provider.name }}
          </button>
        } @empty {
            Il n'y a pas de bouton de connexion
        }
      </div>
    </div>
  `
})
export class LoginComponent implements OnInit {
  // On injecte directement le service public dans le composant
  providerService = inject(ProviderService);

  ngOnInit(): void {
    // On demande au service de charger les données
    this.providerService.loadProviders();
  }
}