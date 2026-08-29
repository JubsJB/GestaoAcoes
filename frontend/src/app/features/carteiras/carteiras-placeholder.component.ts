import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-carteiras-placeholder',
  template: `
    <section aria-labelledby="carteiras-title">
      <h1 id="carteiras-title">Carteiras</h1>
      <p>Esta área será implementada em uma próxima etapa.</p>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CarteirasPlaceholderComponent {}
