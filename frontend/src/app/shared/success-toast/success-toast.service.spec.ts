import { BreakpointObserver } from '@angular/cdk/layout';
import { OverlayContainer, OverlayModule } from '@angular/cdk/overlay';
import { TestBed } from '@angular/core/testing';

import {
  SUCCESS_TOAST_DESKTOP_TOP,
  SUCCESS_TOAST_DURATION_MS,
  SUCCESS_TOAST_MOBILE_QUERY,
  SUCCESS_TOAST_MOBILE_TOP,
  SuccessToastService
} from './success-toast.service';

describe('SuccessToastService', () => {
  let mobile = false;
  let service: SuccessToastService;
  let overlayContainer: OverlayContainer;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [OverlayModule],
      providers: [
        SuccessToastService,
        { provide: BreakpointObserver, useValue: { isMatched: (query: string) => query === SUCCESS_TOAST_MOBILE_QUERY && mobile } }
      ]
    });
    service = TestBed.inject(SuccessToastService);
    overlayContainer = TestBed.inject(OverlayContainer);
  });

  afterEach(() => {
    service.dismiss();
    overlayContainer.getContainerElement().replaceChildren();
    mobile = false;
    vi.useRealTimers();
  });

  it('posiciona o pane real do overlay no topo direito do desktop e descarta em seis segundos', () => {
    vi.useFakeTimers();
    service.show('Operação concluída.');

    const pane = overlayContainer.getContainerElement().querySelector('.cdk-overlay-pane.app-success-toast-overlay') as HTMLElement;
    const wrapper = pane.parentElement as HTMLElement;
    expect(pane).toBeTruthy();
    expect(wrapper.classList).toContain('cdk-global-overlay-wrapper');
    expect(wrapper.style.alignItems).toBe('flex-start');
    expect(wrapper.style.justifyContent).toBe('flex-end');
    expect(pane.style.marginTop).toBe(SUCCESS_TOAST_DESKTOP_TOP);
    expect(pane.style.marginRight).toBe('1rem');
    expect(pane.style.marginBottom).toBe('');
    expect(pane.textContent).toContain('Operação concluída.');
    expect(pane.querySelector('[role="status"]')?.getAttribute('aria-live')).toBe('polite');

    vi.advanceTimersByTime(SUCCESS_TOAST_DURATION_MS - 1);
    expect(overlayContainer.getContainerElement().querySelector('.app-success-toast-overlay')).toBeTruthy();
    vi.advanceTimersByTime(1);
    expect(overlayContainer.getContainerElement().querySelector('.app-success-toast-overlay')).toBeNull();
  });

  it('ancora o pane mobile ao topo com margens laterais iguais e sem posição inferior', () => {
    mobile = true;
    service.show('Cadastro concluído.');

    const pane = overlayContainer.getContainerElement().querySelector('.cdk-overlay-pane.app-success-toast-overlay') as HTMLElement;
    const wrapper = pane.parentElement as HTMLElement;
    expect(wrapper.style.alignItems).toBe('flex-start');
    expect(wrapper.style.justifyContent).toBe('flex-end');
    expect(pane.style.marginTop).toBe(SUCCESS_TOAST_MOBILE_TOP);
    expect(pane.style.marginRight).toBe('1rem');
    expect(pane.style.width).toBe('calc(100vw - 2rem)');
    expect(pane.style.marginBottom).toBe('');
  });
});
