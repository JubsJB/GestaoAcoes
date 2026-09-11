import { CARTEIRAS_ROUTES } from './carteiras.routes';

describe('CARTEIRAS_ROUTES', () => {
  it('preserva precedência das rotas estáticas e os quatro destinos lazy', () => {
    expect(CARTEIRAS_ROUTES.map((route) => route.path)).toEqual(['', 'nova', ':id/editar', ':id']);
    expect(CARTEIRAS_ROUTES.every((route) => typeof route.loadComponent === 'function')).toBe(true);
  });
  it('define os modos das rotas diretas do formulário', () => {
    expect(CARTEIRAS_ROUTES[1].data?.['mode']).toBe('create');
    expect(CARTEIRAS_ROUTES[2].data?.['mode']).toBe('edit');
  });
});
