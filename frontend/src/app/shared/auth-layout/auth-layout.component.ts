import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AppIconComponent } from '../app-icon/app-icon.component';

@Component({
  selector: 'app-auth-layout',
  host: { class: 'app-blue-scope' },
  imports: [AppIconComponent],
  template: `<div class="auth-layout">
    <aside class="institutional" aria-label="Gestão de Ações">
      <div class="identity"><span class="mark" aria-hidden="true"><app-icon name="wallet" /></span><span>Gestão de Ações<small>Investimentos com clareza</small></span></div>
      <div class="institutional-copy"><p class="eyebrow">PERSPECTIVA PARA INVESTIR</p><h2>Seu patrimônio.<br>Uma visão mais clara.</h2><p>Acompanhe suas carteiras, organize suas operações e veja cada posição com mais contexto.</p></div>
      <div class="abstract" aria-hidden="true"><span></span><span></span><span></span><div class="abstract-card"><app-icon name="portfolio" /><i></i><i></i><i></i></div></div>
      <p class="institutional-footer">Organização para acompanhar cada decisão.</p>
    </aside>
    <main class="auth-main"><ng-content /></main>
  </div>`,
  styleUrl: './auth-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthLayoutComponent {}
