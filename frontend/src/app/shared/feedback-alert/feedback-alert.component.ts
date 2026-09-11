import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AppIconComponent, AppIconName } from '../app-icon/app-icon.component';

export type FeedbackVariant = 'success' | 'info' | 'warning' | 'error';

@Component({
  selector: 'app-feedback-alert',
  imports: [AppIconComponent],
  template: `
    <section
      class="feedback-alert feedback-alert--{{ variant() }}"
      [attr.role]="role()"
      [attr.aria-live]="ariaLive()"
    >
      <span class="feedback-alert__marker" aria-hidden="true"><app-icon [name]="icon()" /></span>
      <div>
        <p class="feedback-alert__message">{{ message() }}</p>
        @if (detailEntries().length) {
          <dl>
            @for (entry of detailEntries(); track entry[0]) {
              <dt>{{ entry[0] }}</dt>
              <dd>{{ entry[1] }}</dd>
            }
          </dl>
        }
      </div>
    </section>
  `,
  styleUrl: './feedback-alert.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FeedbackAlertComponent {
  readonly variant = input<FeedbackVariant>('info');
  readonly message = input.required<string>();
  readonly details = input<Record<string, unknown> | null>(null);
  readonly urgent = input<boolean | null>(null);

  protected readonly role = computed(() => this.isUrgent() ? 'alert' : 'status');
  protected readonly ariaLive = computed(() => this.isUrgent() ? 'assertive' : 'polite');
  protected readonly icon = computed<AppIconName>(() => this.variant());
  protected readonly detailEntries = computed<[string, string][]>(() =>
    Object.entries(this.details() ?? {}).map(([key, value]) => [
      key,
      typeof value === 'string' ? value : JSON.stringify(value)
    ])
  );

  private isUrgent(): boolean {
    return this.urgent() ?? (this.variant() === 'error' || this.variant() === 'warning');
  }
}
