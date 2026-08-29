import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-operacoes-placeholder',
  template: `
    <section aria-labelledby="operacoes-title">
      <h1 id="operacoes-title">Operações</h1>
      <p>Esta área será implementada em uma próxima etapa.</p>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperacoesPlaceholderComponent {}
