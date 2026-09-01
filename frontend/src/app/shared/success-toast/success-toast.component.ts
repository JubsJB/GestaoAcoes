import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

import { AppIconComponent } from '../app-icon/app-icon.component';
import { SUCCESS_TOAST_DATA, SuccessToastRef } from './success-toast.tokens';

@Component({
  selector: 'app-success-toast',
  imports: [AppIconComponent, MatButtonModule],
  template: `
    <div class="success-toast" role="status" aria-live="polite">
      <span class="success-toast__icon" aria-hidden="true"><app-icon name="success" /></span>
      <span class="success-toast__message">{{ data.message }}</span>
      <button mat-button type="button" aria-label="Fechar mensagem de sucesso" (click)="dismiss()">Fechar</button>
    </div>
  `,
  styles: [`
    :host{display:block;width:100%}.success-toast{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:.75rem;width:100%;padding:.75rem 1rem;border:1px solid #b7d5c2;border-radius:var(--app-card-radius);background:var(--app-surface-card);box-shadow:0 .5rem 1.5rem rgb(31 36 29 / 16%);color:var(--app-text-primary)}
    .success-toast__icon{width:2rem;height:2rem;display:grid;place-items:center;border-radius:999px;color:var(--app-success);background:#e5f3e9}
    .success-toast__message{font-weight:650;line-height:1.4;overflow-wrap:anywhere}
    @media(max-width:36rem){.success-toast{min-width:0;width:100%;grid-template-columns:auto minmax(0,1fr) auto;gap:.5rem}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SuccessToastComponent {
  protected readonly data = inject(SUCCESS_TOAST_DATA);
  private readonly toastRef = inject(SuccessToastRef);

  protected dismiss(): void {
    this.toastRef.dismiss();
  }
}
