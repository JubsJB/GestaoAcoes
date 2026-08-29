import { Routes } from '@angular/router';

import { MainLayoutComponent } from './layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((module) => module.DASHBOARD_ROUTES)
      },
      {
        path: 'corretoras',
        loadChildren: () =>
          import('./features/corretoras/corretoras.routes').then((module) => module.CORRETORAS_ROUTES)
      },
      {
        path: 'acoes',
        loadChildren: () =>
          import('./features/acoes/acoes.routes').then((module) => module.ACOES_ROUTES)
      },
      {
        path: 'carteiras',
        loadChildren: () =>
          import('./features/carteiras/carteiras.routes').then((module) => module.CARTEIRAS_ROUTES)
      },
      {
        path: 'operacoes',
        loadChildren: () =>
          import('./features/operacoes/operacoes.routes').then((module) => module.OPERACOES_ROUTES)
      },
      {
        path: '**',
        loadComponent: () =>
          import('./layout/not-found/not-found.component').then((module) => module.NotFoundComponent)
      }
    ]
  }
];
