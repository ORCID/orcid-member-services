import { FormControl } from "@angular/forms";

export enum EventType {
  LOG_IN_SUCCESS = 'LOG_IN_SUCCESS',
  AFFILIATION_CREATED = 'AFFILIATION_CREATED',
  AFFILIATION_UPDATED = 'AFFILIATION_UPDATED',
  USER_LIST_MODIFIED = 'USER_LIST_MODIFIED',
}

export enum AlertType {
  SEND_ACTIVATION_SUCCESS = 'Invite sent.',
  SEND_ACTIVATION_FAILURE = 'Invite email couldn`t be sent.',
}

export const EMAIL_NOT_FOUND_TYPE = 'https://www.jhipster.tech/problem/email-not-found'

export const DATE_FORMAT = 'YYYY-MM-DD'
export const DATE_TIME_FORMAT = 'YYYY-MM-DDTHH:mm'

export const ITEMS_PER_PAGE = 20

export function emailValidator(control: FormControl): { [key: string]: any } | null {
  const emailRegexp = /^([^@\s\."'\(\)\[\]\{\}\\/,:;]+\.)*([^@\s\."\(\)\[\]\{\}\\/,:;]|(".+"))+@[^@\s\."'\(\)\[\]\{\}\\/,:;]+(\.[^@\s\."'\(\)\[\]\{\}\\/,:;]{2,})+$/;
  if (control.value && !emailRegexp.test(control.value)) {
    return { invalidEmail: true };
  }
  return null
}