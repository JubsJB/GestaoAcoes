import { AuthBrowser, SessionSignal } from '../src/app/core/auth/auth-browser';

export class BrowserFixture implements AuthBrowser {
  shown = true;
  readonly listeners = new Map<string, (event: Event) => void>();
  readonly sent: SessionSignal[] = [];
  receive?: (message: unknown) => void;
  visible(): boolean { return this.shown; }
  listen(type: string, listener: (event: Event) => void): () => void { this.listeners.set(type, listener); return () => { this.listeners.delete(type); }; }
  mask(_masked: boolean): void {}
  connect(receive: (message: unknown) => void) { this.receive = receive; return { send: (signal: SessionSignal) => { this.sent.push(signal); }, close: () => {} }; }
  emit(type: string, extra: Record<string, unknown> = {}): void { this.listeners.get(type)?.({ type, isTrusted: true, target: document.createElement('button'), ...extra } as unknown as Event); }
}
