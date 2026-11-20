import { HttpErrorResponse } from '@angular/common/http';
import { DestroyRef, inject, Injectable, signal } from '@angular/core';

/** Estado transitório de apresentação; os limites efetivos continuam no backend. */
@Injectable()
export class RetryAfterState {
  private readonly seconds = signal(0);
  readonly remaining = this.seconds.asReadonly();
  private timer?: ReturnType<typeof setInterval>;

  constructor() { inject(DestroyRef).onDestroy(() => this.stop()); }

  apply(error: unknown): void {
    if (!error || typeof error !== 'object' || !('status' in error) || error.status !== 429) return;
    const original = error instanceof HttpErrorResponse ? error : 'originalError' in error && error.originalError instanceof HttpErrorResponse ? error.originalError : null;
    const header = original?.headers.get('Retry-After');
    if (!header || !/^\d+$/.test(header)) return;
    const duration = Number(header);
    const deadline = Date.now() + duration * 1000;
    if (!Number.isSafeInteger(duration) || !Number.isSafeInteger(deadline) || duration <= 0) return;
    this.stop();
    const update = () => {
      this.seconds.set(Math.max(0, Math.ceil((deadline - Date.now()) / 1000)));
      if (this.seconds() === 0) this.stop();
    };
    update();
    this.timer = setInterval(update, 1000);
  }

  private stop(): void { if (this.timer !== undefined) clearInterval(this.timer); this.timer = undefined; }
}
