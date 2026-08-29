import { Routes } from '@angular/router';

export const OPERACOES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./operacoes-placeholder.component').then((module) => module.OperacoesPlaceholderComponent)
  }
];
