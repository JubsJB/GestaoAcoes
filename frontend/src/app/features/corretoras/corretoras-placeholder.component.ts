import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-corretoras-placeholder',
  template: `
    <section aria-labelledby="corretoras-title">
      <h1 id="corretoras-title">Corretoras</h1>
      <p>Esta área será implementada em uma próxima etapa.</p>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CorretorasPlaceholderComponent {}
