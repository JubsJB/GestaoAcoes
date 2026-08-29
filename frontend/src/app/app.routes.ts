import { Routes } from '@angular/router';

import { BaselineReadyComponent } from './layout/baseline-ready.component';

export const routes: Routes = [
  { path: '', component: BaselineReadyComponent, pathMatch: 'full' },
  { path: '**', redirectTo: '' }
];
