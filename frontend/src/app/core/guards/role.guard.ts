import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { UserRole } from '../constants/role.constants';
import { SessionService } from '../services/session.service';

export const roleGuard: CanActivateFn = (route) => {
  const session = inject(SessionService);
  const router = inject(Router);
  const expected = (route.data?.['roles'] as UserRole[] | undefined) ?? [];
  const role = session.role();

  if (!role) {
    return router.createUrlTree(['/login']);
  }

  return expected.length === 0 || expected.includes(role)
    ? true
    : router.createUrlTree(['/unauthorized']);
};
