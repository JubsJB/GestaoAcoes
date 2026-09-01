import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AppIconComponent, AppIconName } from '../app-icon/app-icon.component';

@Component({
  selector: 'app-page-header',
  imports: [AppIconComponent],
  template: `
    <header class="page-header">
      @if (icon()) { <span class="page-header__icon" aria-hidden="true"><app-icon [name]="icon()!" /></span> }
      <div class="page-header__copy">
        @if (eyebrow()) { <p class="page-header__eyebrow">{{ eyebrow() }}</p> }
        <h1 [id]="headingId()">{{ title() }}</h1>
        @if (description()) {
          <p class="page-header__description">{{ description() }}</p>
        }
      </div>
      <div class="page-header__action">
        <ng-content select="[page-header-action]" />
      </div>
    </header>
  `,
  styleUrl: './page-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PageHeaderComponent {
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
  readonly eyebrow = input<string | null>(null);
  readonly icon = input<AppIconName | null>(null);
  readonly headingId = input('page-title');
}
