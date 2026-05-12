import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function indianMobileValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = String(control.value ?? '').trim();
    if (!value) {
      return { mobile: true };
    }
    return /^[6-9]\d{9}$/.test(value) ? null : { indianMobile: true };
  };
}
