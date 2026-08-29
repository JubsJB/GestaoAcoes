import { Routes } from '@angular/router';

export const CARTEIRAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./carteiras-placeholder.component').then((module) => module.CarteirasPlaceholderComponent)
  }
];
