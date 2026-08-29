export interface NavigationItem {
  readonly label: string;
  readonly route: string;
}

export const NAVIGATION_ITEMS: readonly NavigationItem[] = [
  { label: 'Dashboard', route: '/dashboard' },
  { label: 'Corretoras', route: '/corretoras' },
  { label: 'Ações', route: '/acoes' },
  { label: 'Carteiras', route: '/carteiras' },
  { label: 'Operações', route: '/operacoes' }
];
