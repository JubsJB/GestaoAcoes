import { Routes } from '@angular/router';

export const OPERACOES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/operacoes-list-page.component').then((module) => module.OperacoesListPageComponent)
  },
  {
    path: 'nova',
    loadComponent: () =>
      import('./pages/operacao-form-page.component').then((module) => module.OperacaoFormPageComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/operacao-detail-page.component').then((module) => module.OperacaoDetailPageComponent)
  }
];
