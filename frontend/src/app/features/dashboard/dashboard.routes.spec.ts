import { describe, expect, it } from 'vitest';

import { DASHBOARD_ROUTES } from './dashboard.routes';
import { DashboardPageComponent } from './dashboard-page.component';

describe('dashboard routes', () => {
  it('mantém o limite lazy e carrega a página funcional', async () => {
    expect(DASHBOARD_ROUTES[0].path).toBe('');
    const loader = DASHBOARD_ROUTES[0].loadComponent as () => Promise<{ name: string }>;
    const component = await loader();
    expect(component).toBe(DashboardPageComponent);
  });
});
