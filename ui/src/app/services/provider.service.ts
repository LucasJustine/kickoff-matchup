import { Service, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { AuthProvider } from '../models/auth-provider.model';

@Service()
export class ProviderService {
    private http = inject(HttpClient);
    private readonly API_URL = 'http://localhost:8080';

    private providersSignal = signal<AuthProvider[]>([]);

    readonly providers = this.providersSignal.asReadonly();

    loadProviders() {
        if (this.providersSignal().length === 0) {
            this.http.get<AuthProvider[]>(`${this.API_URL}/auth/providers`).pipe(
                tap((data) => this.providersSignal.set(data))
            ).subscribe({
                error: (err) => console.error('Erreur chargement providers', err)
            });
        }
    }


    login(providerUrl: string, returnUrl: string = '/') {
        const fullUrl = `${this.API_URL}${providerUrl}?redirect_uri=${returnUrl}`;
        window.location.href = fullUrl;
    }
}