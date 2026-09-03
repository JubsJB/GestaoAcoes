import { OPERACOES_ROUTES } from './operacoes.routes';
describe('operation routes',()=>it('resolve nova antes do id e mantém o limite lazy',()=>{expect(OPERACOES_ROUTES.map(route=>route.path)).toEqual(['','nova',':id']);expect(OPERACOES_ROUTES.every(route=>typeof route.loadComponent==='function')).toBe(true);}));
