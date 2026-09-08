import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CorretorasService } from './corretoras.service';

/** Page-scoped lookup: one collection request shared by all history rows. */
@Injectable()
export class CorretoraNamesService {
  private readonly service = inject(CorretorasService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly names = signal<ReadonlyMap<number, string>>(new Map());
  private requested = false;

  loadFor(operations: readonly { corretoraId?: number | null }[]): void {
    if (this.requested || !operations.some(item => item.corretoraId != null)) return;
    this.requested = true;
    this.service.listar().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => this.names.set(new Map(items.map(item => [
        item.id, item.nomeFantasia?.trim() || item.razaoSocial.trim()
      ]))),
      // Reference data must never block or replace the operation history.
      error: () => this.names.set(new Map())
    });
  }

  name(id: number | null | undefined): string {
    return id == null ? 'Sem corretora' : this.names().get(id) || `Corretora #${id}`;
  }
}
