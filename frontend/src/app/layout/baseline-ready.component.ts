import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-baseline-ready',
  template: '<p>Frontend inicializado.</p>',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BaselineReadyComponent {}
