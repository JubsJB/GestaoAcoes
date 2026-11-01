import { Routes } from '@angular/router';

import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { adminGuard, sessionGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login-page.component').then(m => m.LoginPageComponent) },
  { path: 'definir-senha', data: { mode: 'initial' }, loadComponent: () => import('./features/auth/password-page.component').then(m => m.PasswordPageComponent) },
  { path: 'redefinir-senha', data: { mode: 'reset' }, loadComponent: () => import('./features/auth/password-page.component').then(m => m.PasswordPageComponent) },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [sessionGuard],
    canActivateChild: [sessionGuard],
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
      { path: 'usuarios', canActivate: [adminGuard], canActivateChild: [adminGuard], loadChildren: () => import('./features/usuarios/usuarios.routes').then(m => m.USUARIOS_ROUTES) },
      {
        path: '**',
        loadComponent: () =>
          import('./layout/not-found/not-found.component').then((module) => module.NotFoundComponent)
      }
    ]
  }
];
