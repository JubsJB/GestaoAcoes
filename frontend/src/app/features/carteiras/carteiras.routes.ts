import { Routes } from '@angular/router';

export const CARTEIRAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/carteiras-list-page.component').then((module) => module.CarteirasListPageComponent)
  },
  {
    path: 'nova',
    data: { mode: 'create' },
    loadComponent: () =>
      import('./pages/carteira-form-page.component').then((module) => module.CarteiraFormPageComponent)
  },
  {
    path: ':id/editar',
    data: { mode: 'edit' },
    loadComponent: () =>
      import('./pages/carteira-form-page.component').then((module) => module.CarteiraFormPageComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/carteira-detail-page.component').then((module) => module.CarteiraDetailPageComponent)
  }
];
