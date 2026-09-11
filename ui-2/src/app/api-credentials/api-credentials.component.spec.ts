import { ComponentFixture, TestBed } from '@angular/core/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'

import { ApiCredentialsComponent } from './api-credentials.component'
import { AccountService } from '../account'
import { of } from 'rxjs'
import { MemberService } from '../member/service/member.service'

const accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
const memberServiceSpy = jasmine.createSpyObj('MemberService', ['getApiCredsForMember'])

describe('ManageApiCredentialsComponent', () => {
  let component: ApiCredentialsComponent
  let fixture: ComponentFixture<ApiCredentialsComponent>

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApiCredentialsComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
      ],
    })
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    component = fixture.componentInstance

    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['ROLE_USER', 'ROLE_ADMIN'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        loggedAs: false,
        loginAs: 'sfid',
        mainContact: false,
        mfaEnabled: false,
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )

    memberServiceSpy.getApiCredsForMember.and.returnValue(of({ content: [], totalElements: 0 } as any))

    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
