import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of } from 'rxjs';
import { SessionService } from './session.service';
import { ReturnNavigationService } from './return-navigation.service';
import { publicRoute } from './session-boundary.service';

export const sessionGuard: CanActivateFn = (route, state) => {
  const session = inject(SessionService);
  const router = inject(Router);
  const returns = inject(ReturnNavigationService);
  const enteringFromPublic = route.parent === route.root && publicRoute(router.url);
  return (session.authenticated() && !enteringFromPublic ? of(true) : session.restore()).pipe(
    map(valid => {
      if (valid) {
        const identity = session.identity();
        if (identity && !returns.mayResume(identity.id)) {
          returns.clear();
          returns.notice.set('O contexto anterior pertence a outra identidade. Escolha novamente a carteira.');
          return router.createUrlTree(['/dashboard']);
        }
        return true;
      }
      returns.capture(state.url, session.previousIdentity);
      return router.createUrlTree(['/login']);
    })
  );
};

export const adminGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  return session.admin() ? true : inject(Router).createUrlTree(['/dashboard']);
};
