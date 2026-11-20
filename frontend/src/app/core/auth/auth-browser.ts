import { InjectionToken } from '@angular/core';

export type SessionSignal = 'SESSION_CHANGED' | 'SESSION_ENDED' | 'ACTIVITY_UPDATED';
export interface AuthBrowser {
  visible(): boolean;
  listen(type: string, listener: (event: Event) => void): () => void;
  mask(masked: boolean): void;
  connect(receive: (message: unknown) => void): { send(signal: SessionSignal): void; close(): void } | null;
}

export const AUTH_BROWSER = new InjectionToken<AuthBrowser>('Authentication browser boundary', {
  providedIn: 'root', factory: () => ({
    visible: () => document.visibilityState === 'visible',
    listen(type, listener) {
      const target = type === 'focus' || type === 'popstate' ? window : document;
      const capture = type !== 'focus';
      target.addEventListener(type, listener, { capture, passive: true });
      return () => target.removeEventListener(type, listener, capture);
    },
    mask: masked => document.documentElement.classList.toggle('auth-session-masked', masked),
    connect(receive) {
      try {
        if (typeof BroadcastChannel === 'undefined') return null;
        const channel = new BroadcastChannel('gestaoacoes.sessao.v1');
        channel.onmessage = event => receive(event.data);
        return { send: type => channel.postMessage({ type }), close: () => channel.close() };
      } catch { return null; }
    }
  })
});
