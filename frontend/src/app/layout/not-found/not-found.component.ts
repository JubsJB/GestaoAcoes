import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [MatButtonModule, RouterLink],
  template: `
    <section aria-labelledby="not-found-title">
      <h1 id="not-found-title">Página não encontrada</h1>
      <p>O endereço informado não corresponde a uma área disponível.</p>
      <a matButton="filled" routerLink="/dashboard">Voltar para o Dashboard</a>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotFoundComponent {}
