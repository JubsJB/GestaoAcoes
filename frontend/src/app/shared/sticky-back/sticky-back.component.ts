import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-sticky-back',
  imports: [MatButtonModule, RouterLink],
  template: `
    <a class="app-back-action" mat-button [routerLink]="route" [attr.aria-label]="label">
      <span aria-hidden="true">&larr;</span><span class="app-back-action__label">{{ label }}</span>
    </a>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StickyBackComponent {
  @Input({ required: true }) route!: string;
  @Input({ required: true }) label!: string;
}
