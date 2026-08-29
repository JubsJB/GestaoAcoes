import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-dashboard-placeholder',
  template: `
    <section aria-labelledby="dashboard-title">
      <h1 id="dashboard-title">Dashboard</h1>
      <p>Esta área será implementada em uma próxima etapa.</p>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardPlaceholderComponent {}
