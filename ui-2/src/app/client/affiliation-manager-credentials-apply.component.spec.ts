import { ComponentFixture, TestBed } from '@angular/core/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ActivatedRoute, convertToParamMap, Router, RouterModule } from '@angular/router'
import { BehaviorSubject, of, throwError } from 'rxjs'
import { AccountService } from '../account'
import { AlertMessage, AlertType } from '../app.constants'
import { IMember } from '../member/model/member.model'
import { MemberService } from '../member/service/member.service'
import { AlertService } from '../shared/service/alert.service'
import { AffiliationManagerCredentialsApplyComponent } from './affiliation-manager-credentials-apply.component'
import { ApiCredentialsService } from './service/api-credentials.service'

describe('AffiliationManagerCredentialsApplyComponent', () => {
  let component: AffiliationManagerCredentialsApplyComponent
  let fixture: ComponentFixture<AffiliationManagerCredentialsApplyComponent>
  let router: Router
  let alertService: jasmine.SpyObj<AlertService>
  let apiCredentialsService: jasmine.SpyObj<ApiCredentialsService>
  let memberService: jasmine.SpyObj<MemberService>

  const member: IMember = {
    salesforceId: 'memberId',
    parentSalesforceId: 'leadId',
    isConsortiumLead: false,
    clientName: 'Member Org',
  }
  const consortiumLead: IMember = { salesforceId: 'leadId', isConsortiumLead: true, clientName: 'Lead Org' }

  const createComponent = () => {
    fixture = TestBed.createComponent(AffiliationManagerCredentialsApplyComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  }

  const valueOf = (selector: string): string => fixture.nativeElement.querySelector(selector).value

  beforeEach(() => {
    const alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast'])
    const apiCredentialsServiceSpy = jasmine.createSpyObj('ApiCredentialsService', [
      'submitAffiliationManagerCredentialsApplication',
    ])
    apiCredentialsServiceSpy.submitAffiliationManagerCredentialsApplication.and.returnValue(of(void 0))
    const memberServiceSpy = jasmine.createSpyObj('MemberService', ['find', 'getMemberData'])
    memberServiceSpy.find.withArgs('memberId').and.returnValue(of(member))
    memberServiceSpy.find.withArgs('leadId').and.returnValue(of(consortiumLead))
    memberServiceSpy.getMemberData.and.returnValue(of({ website: 'https://member.example.org' }))

    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), AffiliationManagerCredentialsApplyComponent],
      providers: [
        { provide: AlertService, useValue: alertServiceSpy },
        { provide: ApiCredentialsService, useValue: apiCredentialsServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AccountService, useValue: { accountData: new BehaviorSubject({ email: 'requester@example.org' }) } },
        {
          provide: ActivatedRoute,
          useValue: { parent: { snapshot: { paramMap: convertToParamMap({ memberId: 'memberId' }) } } },
        },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
    router = TestBed.inject(Router)
    spyOn(router, 'navigate')
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
    apiCredentialsService = TestBed.inject(ApiCredentialsService) as jasmine.SpyObj<ApiCredentialsService>
    memberService = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
  })

  it('should create', () => {
    createComponent()

    expect(component).toBeTruthy()
  })

  it('should prefill the collated organisation, homepage and requester details', () => {
    createComponent()

    expect(memberService.getMemberData).toHaveBeenCalledWith('memberId')
    expect(valueOf('#field_organizationName')).toBe('Member Org')
    expect(valueOf('#field_consortiumLeadName')).toBe('Lead Org')
    expect(valueOf('#field_requesterEmail')).toBe('requester@example.org')
    expect(valueOf('#field_institutionHomepageUrl')).toBe('https://member.example.org')
  })

  it('should leave the homepage empty when the member data cannot be loaded', () => {
    memberService.getMemberData.and.returnValue(of(null))
    createComponent()

    expect(valueOf('#field_institutionHomepageUrl')).toBe('')
  })

  it('should mark the pre-filled fields as required so a failed lookup cannot be submitted silently', () => {
    createComponent()

    expect(component.applyForm.controls.organizationName.hasError('required')).toBeFalse()
    expect(component.applyForm.controls.consortiumLeadName.hasError('required')).toBeFalse()
    expect(component.applyForm.controls.requesterEmail.hasError('required')).toBeFalse()
    expect(component.applyForm.controls.institutionHomepageUrl.hasError('required')).toBeFalse()
    expect(component.applyForm.valid).toBeTrue()
  })

  it('should not submit and should show a required-field error when a value fails to load', () => {
    memberService.getMemberData.and.returnValue(of(null))
    createComponent()

    component.save()
    fixture.detectChanges()

    expect(apiCredentialsService.submitAffiliationManagerCredentialsApplication).not.toHaveBeenCalled()
    expect(component.applyForm.controls.institutionHomepageUrl.touched).toBeTrue()
    expect(fixture.nativeElement.querySelector('#field_institutionHomepageUrl + small')).not.toBeNull()
  })

  it('should send non-consortium members back to the credentials list', () => {
    memberService.find.withArgs('memberId').and.returnValue(of({ salesforceId: 'memberId', isConsortiumLead: false }))
    createComponent()

    expect(router.navigate).toHaveBeenCalledWith(['/api-credentials', 'memberId'])
    expect(memberService.find).not.toHaveBeenCalledWith('leadId')
  })

  it('should show an error when the organisation details cannot be loaded', () => {
    memberService.find.withArgs('memberId').and.returnValue(throwError(() => new Error('failed')))
    createComponent()

    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.API_CREDENTIAL_MEMBER_LOAD_ERROR)
  })

  it('should submit the trimmed notes, then broadcast a toast and navigate to the list', () => {
    createComponent()
    component.applyForm.controls.notes.setValue('  Please call us  ')

    component.save()

    expect(apiCredentialsService.submitAffiliationManagerCredentialsApplication).toHaveBeenCalledWith({
      notes: 'Please call us',
    })
    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.API_CREDENTIAL_APPLICATION_SUBMITTED)
    expect(router.navigate).toHaveBeenCalledWith(['/api-credentials', 'memberId'])
  })

  it('should submit an empty application when notes are blank', () => {
    createComponent()
    component.applyForm.controls.notes.setValue('   ')

    component.save()

    expect(apiCredentialsService.submitAffiliationManagerCredentialsApplication).toHaveBeenCalledWith({})
  })

  it('should stay on the form and show an error when submission fails', () => {
    apiCredentialsService.submitAffiliationManagerCredentialsApplication.and.returnValue(
      throwError(() => new Error('The service is unavailable'))
    )
    createComponent()

    component.save()

    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.API_CREDENTIAL_APPLICATION_ERROR)
    expect(router.navigate).not.toHaveBeenCalled()
  })
})
