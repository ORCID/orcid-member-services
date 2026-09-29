import { ComponentFixture, TestBed } from '@angular/core/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ActivatedRoute, convertToParamMap, RouterModule } from '@angular/router'
import { of, throwError } from 'rxjs'

import { ApiCredentialsComponent } from './api-credentials.component'
import { ApiCredentialsService } from './service/api-credentials.service'
import { Client } from './model/client'
import { MemberService } from '../member/service/member.service'

type ApiCredentialsComponentInternals = {
  productionCredentials: () => Client[]
  sandboxCredentials: () => Client[]
}

const internals = (component: ApiCredentialsComponent): ApiCredentialsComponentInternals =>
  component as unknown as ApiCredentialsComponentInternals

describe('ManageApiCredentialsComponent', () => {
  let component: ApiCredentialsComponent
  let fixture: ComponentFixture<ApiCredentialsComponent>
  let apiCredentialsService: jasmine.SpyObj<ApiCredentialsService>
  let memberService: jasmine.SpyObj<MemberService>

  const mockClients: Client[] = [{ clientId: 'abc123', clientName: 'Test', editable: true }]

  beforeEach(() => {
    const apiCredentialsServiceSpy = jasmine.createSpyObj('ApiCredentialsService', ['getClientsForMember'])
    apiCredentialsServiceSpy.getClientsForMember.and.returnValue(of(mockClients))
    const memberServiceSpy = jasmine.createSpyObj('MemberService', ['find'])
    memberServiceSpy.find.and.returnValue(
      of({ salesforceId: 'memberId', parentSalesforceId: 'leadId', isConsortiumLead: false })
    )
    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), ApiCredentialsComponent],
      providers: [
        { provide: ApiCredentialsService, useValue: apiCredentialsServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ memberId: 'memberId' }) } } },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
    apiCredentialsService = TestBed.inject(ApiCredentialsService) as jasmine.SpyObj<ApiCredentialsService>
    memberService = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should populate production credentials from the clients of the route member', () => {
    expect(apiCredentialsService.getClientsForMember).toHaveBeenCalledWith('memberId')
    expect(internals(component).productionCredentials()).toEqual(mockClients)
  })

  it('should keep sandbox credentials empty when search fails', () => {
    apiCredentialsService.getClientsForMember.and.returnValue(throwError(() => new Error('failed')))
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    expect(internals(component).sandboxCredentials()).toEqual([])
  })

  it('should not call search when no route member id is available', () => {
    const activatedRoute = TestBed.inject(ActivatedRoute)
    activatedRoute.snapshot = { paramMap: convertToParamMap({}) } as ActivatedRoute['snapshot']
    apiCredentialsService.getClientsForMember.calls.reset()
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    expect(apiCredentialsService.getClientsForMember).not.toHaveBeenCalled()
  })

  const applyAffiliationManagerButton = () => fixture.nativeElement.querySelector('#applyAffiliationManagerCredentials')

  const recreate = () => {
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  }

  it('should show the Affiliation Manager button for a consortium member', () => {
    expect(memberService.find).toHaveBeenCalledWith('memberId')
    expect(applyAffiliationManagerButton()).not.toBeNull()
  })

  it('should hide the Affiliation Manager button for a direct member', () => {
    memberService.find.and.returnValue(of({ salesforceId: 'memberId', isConsortiumLead: false }))
    recreate()

    expect(applyAffiliationManagerButton()).toBeNull()
  })

  it('should hide the Affiliation Manager button for a consortium lead', () => {
    memberService.find.and.returnValue(of({ salesforceId: 'memberId', parentSalesforceId: 'memberId', isConsortiumLead: true }))
    recreate()

    expect(applyAffiliationManagerButton()).toBeNull()
  })

  it('should hide the Affiliation Manager button when the member cannot be loaded', () => {
    memberService.find.and.returnValue(throwError(() => new Error('failed')))
    recreate()

    expect(applyAffiliationManagerButton()).toBeNull()
  })
})


