import { CORRETORAS_ROUTES } from './corretoras.routes';

describe('CORRETORAS_ROUTES',()=>{
  it('mantém lista, nova e detalhe nesta ordem',()=>expect(CORRETORAS_ROUTES.map(route=>route.path)).toEqual(['','nova',':id']));
  it('mantém os três destinos carregados sob demanda',()=>expect(CORRETORAS_ROUTES.every(route=>typeof route.loadComponent==='function')).toBe(true));
});
