import { Routes } from '@angular/router';

export const CORRETORAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/corretoras-list-page.component').then((module) => module.CorretorasListPageComponent)
  },
  {
    path: 'nova',
    loadComponent: () =>
      import('./pages/corretora-create-page.component').then((module) => module.CorretoraCreatePageComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/corretora-detail-page.component').then((module) => module.CorretoraDetailPageComponent)
  }
];
