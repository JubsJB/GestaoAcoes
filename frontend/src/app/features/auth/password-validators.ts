import { ValidatorFn } from '@angular/forms';

export const passwordPolicy: ValidatorFn = control => {
  const value: unknown = control.value;
  if (value === '') return null;
  return typeof value === 'string' && Array.from(value).length >= 8 && new TextEncoder().encode(value).length <= 72 ? null : { passwordPolicy: true };
};

export const matchingPasswords: ValidatorFn = group =>
  group.get('senha')?.value === group.get('confirmacao')?.value ? null : { confirmationMismatch: true };
