import { FormControl } from '@angular/forms';

export function emailValidator(control: FormControl): { [key: string]: any } {
  const emailRegexp = /^([^@\s\."'\(\)\[\]\{\}\\/,:;]+\.)*([^@\s\."\(\)\[\]\{\}\\/,:;]|(".+"))+@[^@\s\."'\(\)\[\]\{\}\\/,:;]+(\.[^@\s\."'\(\)\[\]\{\}\\/,:;]{2,})+$/;
  if (control.value && !emailRegexp.test(control.value)) {
    return { invalidEmail: true };
  }
}
