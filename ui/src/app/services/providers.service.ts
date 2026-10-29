import { HttpClient } from '@angular/common/http';
import { Injectable, Service, inject, signal } from '@angular/core';
import { environment } from '../../environments/environment.development';

export interface Provider {
    id: string;        // "google", "apple"...
    name: string;      // "Google"
    loginUrl: string;  // "/oauth2/authorization/google"
}


@Service()
export class ProvidersService {
    private readonly http = inject(HttpClient);
    private readonly uri = '/providers';

    private _providers = signal<Provider[] | null>(null);
    public readonly providers = this._providers.asReadonly();

    public loadProviders() {
        if (this._providers() === null) {
            this.http.get<Provider[]>(environment.apiUrl + this.uri ).subscribe({
                next: (providers) => this._providers.set(providers),
                error: (err) => {
                    console.error('Failed to load providers', err);
                    this._providers.set([]);
                }
            });
        }
    }
}