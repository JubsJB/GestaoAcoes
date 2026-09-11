import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CarteiraContextService } from '../../core/carteira/carteira-context.service';
import { CarteiraNavigationService } from '../../core/carteira/carteira-navigation.service';

@Component({
  selector: 'app-carteira-selector',
  imports: [RouterLink],
  template: `
    <label for="global-carteira">Carteira</label>
    <select id="global-carteira" [value]="context.activeId() ?? ''" [disabled]="context.loading() || !!context.error() || context.carteiras().length === 0" (change)="select($event)" aria-describedby="global-carteira-status">
      @if (context.activeId() === null) { <option value="" disabled>Selecione uma carteira</option> }
      @for (item of context.carteiras(); track item.id) { <option [value]="item.id" [selected]="item.id === context.activeId()">{{ item.nome }}</option> }
    </select>
    <span id="global-carteira-status" role="status" aria-live="polite">
      @if (context.loading()) { Carregando carteiras… }
      @else if (context.error()) { {{ context.error()!.message }} <button type="button" (click)="retry()">Tentar novamente</button> }
      @else if (context.invalidSelection()) { Carteira indicada indisponível. Escolha outra carteira. }
      @else if (context.carteiras().length === 0) { <a routerLink="/carteiras/nova">Criar carteira</a> }
    </span>
  `,
  styles: [`
    :host{display:grid;grid-template-columns:auto minmax(0,1fr);align-items:center;gap:.125rem .5rem;min-width:0;width:clamp(9rem,30vw,22rem);font-size:.8rem;line-height:1.2}
    label{font-weight:600}select{min-width:0;width:100%;min-height:2rem;font:inherit;color:var(--app-text-primary);background:var(--app-surface-card);border:1px solid var(--app-border-subtle);border-radius:var(--app-control-radius);padding:.25rem}select:focus-visible,button:focus-visible,a:focus-visible{outline:3px solid var(--app-brand-primary);outline-offset:2px}
    span{grid-column:1/-1;font-size:.7rem;white-space:normal;overflow-wrap:anywhere}span:empty{display:none}button,a{font:inherit;color:var(--app-brand-primary)}button{cursor:pointer;background:none;border:0;text-decoration:underline}
    @media(max-width:36rem){:host{grid-template-columns:minmax(0,1fr)}label{font-size:.7rem}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CarteiraSelectorComponent {
  protected readonly context = inject(CarteiraContextService);
  private readonly navigation = inject(CarteiraNavigationService);
  constructor() { this.navigation.start(); }
  protected select(event: Event): void { this.navigation.select(Number((event.target as HTMLSelectElement).value)); }
  protected retry(): void { this.context.initialize(true).subscribe(); }
}
