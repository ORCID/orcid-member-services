import { AbstractControl, ValidatorFn, Validators } from '@angular/forms'

export const SALESFORCE_ID_PATTERN = /^[a-zA-Z0-9]{18}$/

export function salesforceIdFormatValidator(): ValidatorFn {
  return Validators.pattern(SALESFORCE_ID_PATTERN)
}

export function parentSalesforceIdValidator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: boolean } | null => {
    if (control.parent && control.value) {
      const isConsortiumLead = control.parent.get('isConsortiumLead')?.value
      const salesforceId = control.parent.get('salesforceId')?.value
      if (isConsortiumLead && control.value !== salesforceId) {
        return { validParentSalesforceIdValue: false }
      }
    }
    return null
  }
}

export function clientIdValidator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: boolean } | null => {
    if (control.parent && control.value && isNaN(control.value)) {
      const clientIdValue = control.value
      const isConsortiumLead = control.parent?.get('isConsortiumLead')?.value
      const assertionServiceEnabled = control.parent?.get('assertionServiceEnabled')?.value
      if (!isConsortiumLead && clientIdValue === '') {
        const clientIdControl = control.parent?.get('clientId')
        if (clientIdControl) {
          return Validators.required(clientIdControl)
        }
      }
      if (isConsortiumLead && (!clientIdValue || clientIdValue === '')) {
        return null
      }
      if (!assertionServiceEnabled && (!clientIdValue || clientIdValue === '')) {
        return null
      }
      if (clientIdValue.startsWith('APP-') && clientIdValue.match(/APP-[A-Z0-9]{16}$/)) {
        return null
      } else if (clientIdValue.match(/[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/)) {
        return null
      }
      return { validClientId: false }
    }
    if (control.parent) {
      if (control.parent?.get('isConsortiumLead')?.value) {
        return null
      }
    }
    if (control.parent) {
      if (!control.parent?.get('assertionServiceEnabled')?.value) {
        return null
      }
    }
    return { validClientId: false }
  }
}
