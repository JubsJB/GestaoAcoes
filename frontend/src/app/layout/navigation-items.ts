import { AppIconName } from '../shared/app-icon/app-icon.component';

export interface NavigationItem {
  readonly label: string;
  readonly route: string;
  readonly icon: AppIconName;
}

export const NAVIGATION_ITEMS: readonly NavigationItem[] = [
  { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
  { label: 'Carteiras', route: '/carteiras', icon: 'portfolio' },
  { label: 'Operações', route: '/operacoes', icon: 'operation' },
  { label: 'Ativos', route: '/acoes', icon: 'stock' },
  { label: 'Corretoras', route: '/corretoras', icon: 'broker' }
];
