import { FormControl } from '@angular/forms'
import {
  isValidRedirectUri,
  isValidWebsiteUrl,
  redirectUriListValidator,
  redirectUriValidator,
  websiteUrlValidator,
} from './url.validators'

describe('url validators', () => {
  describe('isValidWebsiteUrl', () => {
    it('should accept http and https URLs with a domain', () => {
      expect(isValidWebsiteUrl('https://example.org')).toBeTrue()
      expect(isValidWebsiteUrl('http://www.example.co.uk/path?x=1#top')).toBeTrue()
      expect(isValidWebsiteUrl('https://sub-domain.example.org:8443/a')).toBeTrue()
    })

    it('should reject values that are not http(s) URLs with a real domain', () => {
      const invalid = [
        'example.org',
        'www.example.org',
        'ftp://example.org',
        'javascript:alert(1)',
        'https://',
        'https://localhost',
        'https://example',
        'https://exa mple.org',
        'https://user:pass@example.org',
        'not a url',
      ]
      invalid.forEach((value) => expect(isValidWebsiteUrl(value)).withContext(value).toBeFalse())
    })
  })

  describe('isValidRedirectUri', () => {
    it('should accept http(s) URIs including localhost and ports', () => {
      expect(isValidRedirectUri('https://example.org/callback')).toBeTrue()
      expect(isValidRedirectUri('http://localhost:4200/callback')).toBeTrue()
      expect(isValidRedirectUri('https://127.0.0.1:8080/cb?x=1')).toBeTrue()
    })

    it('should reject other schemes, missing hosts, bad ports and user info', () => {
      const invalid = [
        'example.org/callback',
        'myapp://callback',
        'https://',
        'https://example.org:99999/cb',
        'https://user@example.org/cb',
        'https://exa_mple.org/cb',
        'https://example.org/call back',
      ]
      invalid.forEach((value) => expect(isValidRedirectUri(value)).withContext(value).toBeFalse())
    })
  })

  describe('websiteUrlValidator', () => {
    const validate = (value: string | null) => websiteUrlValidator()(new FormControl(value))

    it('should leave empty values to the required validator', () => {
      expect(validate(null)).toBeNull()
      expect(validate('   ')).toBeNull()
    })

    it('should flag invalid URLs and ignore surrounding whitespace', () => {
      expect(validate('example.org')).toEqual({ invalidUrl: true })
      expect(validate('  https://example.org  ')).toBeNull()
    })
  })

  describe('redirectUriValidator', () => {
    const validate = (value: string | null) => redirectUriValidator()(new FormControl(value))

    it('should leave empty values to the required validator', () => {
      expect(validate(null)).toBeNull()
      expect(validate('  ')).toBeNull()
    })

    it('should flag a single invalid redirect URI', () => {
      expect(validate('example.org/cb')).toEqual({ invalidRedirectUri: true })
      expect(validate(' http://localhost:3000/cb ')).toBeNull()
    })
  })

  describe('redirectUriListValidator', () => {
    const validate = (value: string | null) => redirectUriListValidator()(new FormControl(value))

    it('should accept an empty value and a list of valid URIs', () => {
      expect(validate(null)).toBeNull()
      expect(validate('https://a.example.org/cb\n\n  http://localhost:3000/cb  \n')).toBeNull()
    })

    it('should report every invalid line', () => {
      expect(validate('https://a.example.org/cb\nnot-a-uri\nmyapp://x')).toEqual({
        invalidRedirectUris: ['not-a-uri', 'myapp://x'],
      })
    })
  })
})
