import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet],
  template: '<main><router-outlet /></main>',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppShellComponent {}
