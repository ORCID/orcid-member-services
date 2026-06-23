import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { ActivatedRoute } from '@angular/router'
import * as KEYUTIL from 'jsrsasign'
import { of } from 'rxjs'
import { Member } from '../member/model/member.model'
import { OrcidRecord } from '../shared/model/orcid-record.model'
import { WindowLocationService } from '../shared/service/window-location.service'
import { LandingPageComponent } from './landing-page.component'
import { LandingPageService } from './landing-page.service'

type LandingPageInternals = {
  processRequest: (state: string, idToken: string, accessToken: string) => void
  checkSubmitToken: (token: string, state: string, accessToken: string) => void
  oauthUrl: string
  showError: boolean
  showDenied: boolean
  showConnectionExists: boolean
  showConnectionExistsDifferentUser: boolean
}

const internals = (component: LandingPageComponent): LandingPageInternals =>
  component as unknown as LandingPageInternals

describe('LandingPageComponent', () => {
  let component: LandingPageComponent
  let fixture: ComponentFixture<LandingPageComponent>
  let landingPageService: jasmine.SpyObj<LandingPageService>
  let windowLocationService: jasmine.SpyObj<WindowLocationService>
  let route: ActivatedRoute

  beforeEach(() => {
    spyOn(console, 'error').and.stub()

    TestBed.configureTestingModule({
      imports: [LandingPageComponent],
      providers: [
        {
          provide: LandingPageService,
          useValue: jasmine.createSpyObj('LandingPageService', [
            'getOrcidConnectionRecord',
            'getMemberInfo',
            'getPublicKey',
            'submitUserResponse',
            'getUserInfo',
            'submitUserResponse',
            'getSalesforceId',
            'getMemberId',
          ]),
        },
        {
          provide: WindowLocationService,
          useValue: jasmine.createSpyObj('WindowLocationService', ['updateWindowLocation', 'getWindowLocationHash']),
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              fragment: '',
              queryParamMap: {
                get: (key: string) => null,
              },
            },
          },
        },
        provideHttpClient(withInterceptorsFromDi()),
      ],
    })
    fixture = TestBed.createComponent(LandingPageComponent)
    component = fixture.componentInstance
    landingPageService = TestBed.inject(LandingPageService) as jasmine.SpyObj<LandingPageService>
    windowLocationService = TestBed.inject(WindowLocationService) as jasmine.SpyObj<WindowLocationService>
    route = TestBed.inject(ActivatedRoute) as ActivatedRoute
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('New record connection should redirect to the registry', () => {
    windowLocationService.updateWindowLocation.and.returnValue()
    landingPageService.getOrcidConnectionRecord.and.returnValue(of(new OrcidRecord('email', 'orcid')))
    landingPageService.getMemberId.and.returnValue(of('memberId'))
    landingPageService.getMemberInfo.and.returnValue(
      of(new Member('id', 'name', 'email', 'orcid', 'memberId', 'clientId'))
    )
    internals(component).processRequest('someState', '', '')
    expect(landingPageService.getOrcidConnectionRecord).toHaveBeenCalled()
    expect(internals(component).oauthUrl).toBe(
      'https://qa.orcid.org/oauth/authorize?response_type=token&redirect_uri=http://localhost:9876/landing-page&client_id=name&scope=/read-limited /activities/update /person/update openid&prompt=login&state=someState'
    )
    expect(landingPageService.getPublicKey).toHaveBeenCalledTimes(0)
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalled()
  })

  it('New record connection should fail (user denied permission)', () => {
    windowLocationService.updateWindowLocation.and.returnValue()
    route.snapshot.fragment = 'error=access_denied'
    landingPageService.getOrcidConnectionRecord.and.returnValue(of(new OrcidRecord('email', 'orcid')))
    landingPageService.getMemberId.and.returnValue(of('memberId'))
    landingPageService.getMemberInfo.and.returnValue(
      of(new Member('id', 'name', 'email', 'orcid', 'memberId', 'clientId'))
    )
    landingPageService.submitUserResponse.and.returnValue(of(''))
    internals(component).processRequest('someState', '', '')
    expect(landingPageService.getOrcidConnectionRecord).toHaveBeenCalled()
    expect(internals(component).oauthUrl).toBe(
      'https://qa.orcid.org/oauth/authorize?response_type=token&redirect_uri=http://localhost:9876/landing-page&client_id=name&scope=/read-limited /activities/update /person/update openid&prompt=login&state=someState'
    )
    expect(landingPageService.getPublicKey).toHaveBeenCalledTimes(0)
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalledTimes(0)
    expect(landingPageService.submitUserResponse).toHaveBeenCalled()
    expect(internals(component).showError).toBeFalsy()
    expect(internals(component).showDenied).toBeTruthy()
  })

  it('New record connection should fail (generic error)', () => {
    windowLocationService.updateWindowLocation.and.returnValue()
    landingPageService.getMemberId.and.returnValue(of('memberId'))
    route.snapshot.fragment = 'error=123'
    landingPageService.getOrcidConnectionRecord.and.returnValue(of(new OrcidRecord('email', 'orcid')))
    landingPageService.getMemberInfo.and.returnValue(
      of(new Member('id', 'name', 'email', 'orcid', 'memberId', 'clientId'))
    )
    landingPageService.submitUserResponse.and.returnValue(of(''))
    internals(component).processRequest('someState', '', '')
    expect(landingPageService.getOrcidConnectionRecord).toHaveBeenCalled()
    expect(internals(component).oauthUrl).toBe(
      'https://qa.orcid.org/oauth/authorize?response_type=token&redirect_uri=http://localhost:9876/landing-page&client_id=name&scope=/read-limited /activities/update /person/update openid&prompt=login&state=someState'
    )
    expect(landingPageService.getPublicKey).toHaveBeenCalledTimes(0)
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalledTimes(0)
    expect(landingPageService.submitUserResponse).toHaveBeenCalled()
    expect(internals(component).showError).toBeTruthy()
    expect(internals(component).showDenied).toBeFalsy()
  })

  it('Existing record connection should be identified', () => {
    windowLocationService.updateWindowLocation.and.returnValue()
    landingPageService.getMemberId.and.returnValue(of('memberId'))
    landingPageService.getOrcidConnectionRecord.and.returnValue(of(new OrcidRecord('email', 'orcid')))
    landingPageService.getMemberInfo.and.returnValue(
      of(new Member('id', 'name', 'email', 'orcid', 'memberId', 'clientId'))
    )
    landingPageService.getPublicKey.and.returnValue(of(['publicKey']))
    landingPageService.submitUserResponse.and.returnValue(of(''))
    landingPageService.getUserInfo.and.returnValue(of({ givenName: 'givenName', familyName: 'familyName' }))
    spyOn(KEYUTIL.KEYUTIL, 'getKey').and.returnValue(new KEYUTIL.RSAKey())
    spyOn(KEYUTIL.KJUR.jws.JWS, 'verifyJWT').and.returnValue(true)

    ;internals(component).processRequest('someState', 'it_token', '')

    expect(landingPageService.getOrcidConnectionRecord).toHaveBeenCalled()
    expect(landingPageService.getPublicKey).toHaveBeenCalled()
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalledTimes(0)
  })

  it('Check for wrong user', () => {
    windowLocationService.updateWindowLocation.and.returnValue()
    landingPageService.submitUserResponse.and.returnValue(of({ isDifferentUser: true }))
    landingPageService.getPublicKey.and.returnValue(of(['publicKey']))
    landingPageService.getUserInfo.and.returnValue(of({ givenName: 'givenName', familyName: 'familyName' }))
    spyOn(KEYUTIL.KEYUTIL, 'getKey').and.returnValue(new KEYUTIL.RSAKey())
    spyOn(KEYUTIL.KJUR.jws.JWS, 'verifyJWT').and.returnValue(true)
    ;internals(component).checkSubmitToken('token', 'state', 'access_token')
    expect(landingPageService.submitUserResponse).toHaveBeenCalled()
    expect(internals(component).showConnectionExists).toBeFalsy()
    expect(internals(component).showConnectionExistsDifferentUser).toBeTruthy()
  })

  it('Check for existing connection', () => {
    windowLocationService.updateWindowLocation.and.returnValue()
    landingPageService.submitUserResponse.and.returnValue(of({ isSameUserThatAlreadyGranted: true }))
    landingPageService.getPublicKey.and.returnValue(of(['publicKey']))
    landingPageService.getUserInfo.and.returnValue(of({ givenName: 'givenName', familyName: 'familyName' }))
    spyOn(KEYUTIL.KEYUTIL, 'getKey').and.returnValue(new KEYUTIL.RSAKey())
    spyOn(KEYUTIL.KJUR.jws.JWS, 'verifyJWT').and.returnValue(true)
    ;internals(component).checkSubmitToken('token', 'state', 'access_token')
    expect(landingPageService.submitUserResponse).toHaveBeenCalled()
    expect(internals(component).showConnectionExists).toBeTruthy()
    expect(internals(component).showConnectionExistsDifferentUser).toBeFalsy()
  })
})
