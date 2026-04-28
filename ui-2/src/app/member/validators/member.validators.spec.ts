import { FormControl, FormGroup } from '@angular/forms'
import { clientIdValidator, parentSalesforceIdValidator, salesforceIdFormatValidator } from './member.validators'

describe('member validators', () => {
  describe('salesforceIdFormatValidator', () => {
    let control: FormControl

    beforeEach(() => {
      control = new FormControl('', [salesforceIdFormatValidator()])
    })

    it('should return null for an empty value', () => {
      control.setValue('')
      expect(control.errors).toBeNull()
    })

    it('should return a pattern error for a value shorter than 18 characters', () => {
      control.setValue('shortid')
      expect(control.errors?.['pattern']).toBeTruthy()
    })

    it('should return a pattern error for a value longer than 18 characters', () => {
      control.setValue('salesforceid0001AAAA')
      expect(control.errors?.['pattern']).toBeTruthy()
    })

    it('should return null for a valid 18 alphanumeric character value', () => {
      control.setValue('salesforceid0001AA')
      expect(control.errors).toBeNull()
    })
  })

  describe('parentSalesforceIdValidator', () => {
    let form: FormGroup

    beforeEach(() => {
      form = new FormGroup({
        isConsortiumLead: new FormControl(false),
        salesforceId: new FormControl(''),
        parentSalesforceId: new FormControl('', [parentSalesforceIdValidator()]),
      })
    })

    it('should return null when the field is empty', () => {
      form.get('parentSalesforceId')?.setValue('')
      expect(form.get('parentSalesforceId')?.errors).toBeNull()
    })

    it('should return an error when consortium lead and parentSalesforceId does not match salesforceId', () => {
      form.get('isConsortiumLead')?.setValue(true)
      form.get('salesforceId')?.setValue('salesforceid0001AA')
      form.get('parentSalesforceId')?.setValue('salesforceid0002AA')
      expect(form.get('parentSalesforceId')?.errors).toEqual({ validParentSalesforceIdValue: false })
    })

    it('should return null when consortium lead and parentSalesforceId matches salesforceId', () => {
      form.get('isConsortiumLead')?.setValue(true)
      form.get('salesforceId')?.setValue('salesforceid0001AA')
      form.get('parentSalesforceId')?.setValue('salesforceid0001AA')
      expect(form.get('parentSalesforceId')?.errors).toBeNull()
    })

    it('should return null when not a consortium lead regardless of parentSalesforceId value', () => {
      form.get('isConsortiumLead')?.setValue(false)
      form.get('salesforceId')?.setValue('salesforceid0001AA')
      form.get('parentSalesforceId')?.setValue('salesforceid0002AA')
      expect(form.get('parentSalesforceId')?.errors).toBeNull()
    })
  })

  describe('clientIdValidator', () => {
    let form: FormGroup

    beforeEach(() => {
      form = new FormGroup({
        isConsortiumLead: new FormControl(false),
        assertionServiceEnabled: new FormControl(false),
        clientId: new FormControl('', [clientIdValidator()]),
      })
    })

    it('should return null for a valid APP- prefixed client id', () => {
      form.get('clientId')?.setValue('APP-0000000000000000')
      expect(form.get('clientId')?.errors).toBeNull()
    })

    it('should return null for a valid hyphenated client id', () => {
      form.get('clientId')?.setValue('0000-0000-0000-0000')
      expect(form.get('clientId')?.errors).toBeNull()
    })

    it('should return an error for an invalid client id', () => {
      form.get('clientId')?.setValue('invalid-id')
      expect(form.get('clientId')?.errors).toEqual({ validClientId: false })
    })

    it('should return null for an empty client id when assertionServiceEnabled is false', () => {
      form.get('assertionServiceEnabled')?.setValue(false)
      form.get('clientId')?.setValue(null)
      expect(form.get('clientId')?.errors).toBeNull()
    })

    it('should return null for an empty client id when isConsortiumLead is true', () => {
      form.get('isConsortiumLead')?.setValue(true)
      form.get('clientId')?.setValue(null)
      expect(form.get('clientId')?.errors).toBeNull()
    })
  })
})
