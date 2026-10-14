import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms'

// Mirrors api-credentials-service: only http/https, and the website needs a real domain.
const HOST_WITH_TLD = /^([a-z0-9]([a-z0-9-]*[a-z0-9])?\.)+[a-z]{2,}$/i
// Redirect URIs also allow localhost and IP hosts, but no user info or odd characters.
const REDIRECT_HOST = /^[a-z0-9.-]+$/i

function parseHttpUrl(value: string): URL | null {
  if (/\s/.test(value) || !/^https?:\/\//i.test(value)) {
    return null
  }
  try {
    const url = new URL(value)
    return url.username || url.password ? null : url
  } catch {
    return null
  }
}

export function isValidWebsiteUrl(value: string): boolean {
  const url = parseHttpUrl(value)
  return !!url && HOST_WITH_TLD.test(url.hostname)
}

export function isValidRedirectUri(value: string): boolean {
  const url = parseHttpUrl(value)
  return !!url && REDIRECT_HOST.test(url.hostname)
}

// Empty values are left to Validators.required.
export function websiteUrlValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = typeof control.value === 'string' ? control.value.trim() : ''
    return !value || isValidWebsiteUrl(value) ? null : { invalidUrl: true }
  }
}

export function redirectUriValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = typeof control.value === 'string' ? control.value.trim() : ''
    return !value || isValidRedirectUri(value) ? null : { invalidRedirectUri: true }
  }
}

// One redirect URI per line; reports the lines that are not valid.
export function redirectUriListValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = typeof control.value === 'string' ? control.value : ''
    const invalid = value
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.length > 0 && !isValidRedirectUri(line))
    return invalid.length > 0 ? { invalidRedirectUris: invalid } : null
  }
}
