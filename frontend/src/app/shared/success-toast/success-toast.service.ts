import { BreakpointObserver } from '@angular/cdk/layout';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { Injectable, Injector, inject } from '@angular/core';

import { SuccessToastComponent } from './success-toast.component';
import { SUCCESS_TOAST_DATA, SuccessToastRef } from './success-toast.tokens';

export const SUCCESS_TOAST_DURATION_MS = 10000;
export const SUCCESS_TOAST_MOBILE_QUERY = '(max-width: 36rem)';
export const SUCCESS_TOAST_DESKTOP_TOP = '4.75rem';
export const SUCCESS_TOAST_MOBILE_TOP = '4.25rem';

@Injectable({ providedIn: 'root' })
export class SuccessToastService {
  private readonly overlay = inject(Overlay);
  private readonly injector = inject(Injector);
  private readonly breakpoints = inject(BreakpointObserver);
  private overlayRef: OverlayRef | null = null;
  private dismissTimer: ReturnType<typeof setTimeout> | null = null;

  show(message: string): void {
    this.dismiss();
    const isMobile = this.breakpoints.isMatched(SUCCESS_TOAST_MOBILE_QUERY);
    const position = this.overlay.position().global()
      .top(isMobile ? SUCCESS_TOAST_MOBILE_TOP : SUCCESS_TOAST_DESKTOP_TOP)
      .right('1rem');
    const overlayRef = this.overlay.create({
      positionStrategy: position,
      width: isMobile ? 'calc(100vw - 2rem)' : undefined,
      scrollStrategy: this.overlay.scrollStrategies.noop(),
      panelClass: 'app-success-toast-overlay',
      hasBackdrop: false,
      disposeOnNavigation: true
    });
    const toastRef = new SuccessToastRef(() => this.dismiss());
    const toastInjector = Injector.create({ parent: this.injector, providers: [
      { provide: SUCCESS_TOAST_DATA, useValue: { message } },
      { provide: SuccessToastRef, useValue: toastRef }
    ] });
    const componentRef = overlayRef.attach(new ComponentPortal(SuccessToastComponent, null, toastInjector));
    componentRef.changeDetectorRef.detectChanges();
    overlayRef.updatePosition();
    this.overlayRef = overlayRef;
    this.dismissTimer = setTimeout(() => this.dismiss(), SUCCESS_TOAST_DURATION_MS);
  }

  dismiss(): void {
    if (this.dismissTimer !== null) {
      clearTimeout(this.dismissTimer);
      this.dismissTimer = null;
    }
    this.overlayRef?.dispose();
    this.overlayRef = null;
  }
}
