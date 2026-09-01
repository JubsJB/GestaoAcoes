import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type AppIconName =
  | 'dashboard' | 'broker' | 'stock' | 'portfolio' | 'operation' | 'wallet'
  | 'identity' | 'location' | 'status' | 'quote' | 'search' | 'empty'
  | 'success' | 'info' | 'warning' | 'error';

@Component({
  selector: 'app-icon',
  template: `<svg viewBox="0 0 24 24" focusable="false" aria-hidden="true"><use [attr.href]="href()" /></svg>`,
  styles: [`:host{width:1.25rem;height:1.25rem;display:inline-grid;place-items:center;flex:0 0 auto;color:inherit}:host svg{width:100%;height:100%;display:block;fill:currentColor}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppIconComponent {
  readonly name = input.required<AppIconName>();
  protected href(): string { return `/app-icons.svg#${this.name()}`; }
}
