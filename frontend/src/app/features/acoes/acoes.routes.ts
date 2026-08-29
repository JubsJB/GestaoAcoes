import { Routes } from '@angular/router';

export const ACOES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./acoes-placeholder.component').then((module) => module.AcoesPlaceholderComponent)
  }
];
