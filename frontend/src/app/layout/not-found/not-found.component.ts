import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

import { PageHeaderComponent } from '../../shared/page-header/page-header.component';

@Component({
  selector: 'app-not-found',
  imports: [MatButtonModule, RouterLink, PageHeaderComponent],
  template: `
    <section class="app-page" aria-labelledby="not-found-title">
      <app-page-header headingId="not-found-title" title="Página não encontrada" description="O endereço informado não corresponde a uma área disponível." />
      <a matButton="filled" routerLink="/dashboard">Voltar para o Dashboard</a>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotFoundComponent {}
