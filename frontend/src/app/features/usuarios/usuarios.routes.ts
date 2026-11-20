import { Routes } from '@angular/router';
export const USUARIOS_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./usuarios-list-page.component').then(m => m.UsuariosListPageComponent) },
  { path: 'novo', loadComponent: () => import('./usuario-create-page.component').then(m => m.UsuarioCreatePageComponent) },
  { path: ':id', loadComponent: () => import('./usuario-detail-page.component').then(m => m.UsuarioDetailPageComponent) }
];
