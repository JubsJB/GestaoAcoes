import { InjectionToken } from '@angular/core';

export interface SuccessToastData {
  readonly message: string;
}

export const SUCCESS_TOAST_DATA = new InjectionToken<SuccessToastData>('SUCCESS_TOAST_DATA');

export class SuccessToastRef {
  constructor(private readonly close: () => void) {}

  dismiss(): void {
    this.close();
  }
}
