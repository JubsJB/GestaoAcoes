import { Routes } from '@angular/router';

export const ACOES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/acoes-list-page.component').then((module) => module.AcoesListPageComponent)
  },
  {
    path: 'nova',
    loadComponent: () =>
      import('./pages/acao-create-page.component').then((module) => module.AcaoCreatePageComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/acao-detail-page.component').then((module) => module.AcaoDetailPageComponent)
  }
];
