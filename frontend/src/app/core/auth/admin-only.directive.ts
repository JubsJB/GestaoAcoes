import { Directive, effect, inject, TemplateRef, ViewContainerRef } from '@angular/core';
import { SessionService } from './session.service';

@Directive({ selector: '[appAdmin]' })
export class AdminOnlyDirective {
  private readonly session = inject(SessionService);
  private readonly template = inject(TemplateRef<unknown>);
  private readonly view = inject(ViewContainerRef);
  private rendered = false;
  constructor() {
    effect(() => {
      if (this.session.admin() === this.rendered) return;
      this.rendered = this.session.admin();
      this.view.clear();
      if (this.rendered) this.view.createEmbeddedView(this.template);
    });
  }
}
