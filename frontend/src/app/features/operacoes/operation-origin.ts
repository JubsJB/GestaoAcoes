import { ParamMap, Router, UrlTree } from '@angular/router';
import { carteiraId } from '../../core/carteira/carteira-context.service';

export function operationReturnUrl(router: Router, params: ParamMap, actualId?: number): UrlTree {
  const id = carteiraId(params.get('carteiraId'));
  if (id === null || (actualId !== undefined && actualId !== id)) return router.parseUrl('/operacoes');
  switch (params.get('origem')) {
    case 'dashboard': return router.createUrlTree(['/dashboard'], { queryParams: { carteiraId: id } });
    case 'carteira': return router.createUrlTree(['/carteiras', id]);
    default: return router.parseUrl('/operacoes');
  }
}
