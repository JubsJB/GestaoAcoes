import { Routes } from '@angular/router';

export const CORRETORAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./corretoras-placeholder.component').then((module) => module.CorretorasPlaceholderComponent)
  }
];
