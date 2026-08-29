import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-acoes-placeholder',
  template: `
    <section aria-labelledby="acoes-title">
      <h1 id="acoes-title">Ações</h1>
      <p>Esta área será implementada em uma próxima etapa.</p>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AcoesPlaceholderComponent {}
