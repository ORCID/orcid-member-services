import { ComponentFixture, TestBed } from '@angular/core/testing'
import { HttpClientModule } from '@angular/common/http'
import { LandingPageComponent } from './landing-page.component'
import { LandingPageService } from './landing-page.service'
import { OrcidRecord } from '../shared/model/orcid-record.model'
import { of } from 'rxjs'
import { Member } from '../member/model/member.model'
import { WindowLocationService } from '../shared/service/window-location.service'
import * as KEYUTIL from 'jsrsasign'

describe('LandingPageComponent', () => {
  let component: LandingPageComponent
  let fixture: ComponentFixture<LandingPageComponent>
  let landingPageService: jasmine.SpyObj<LandingPageService>
  let windowLocationService: jasmine.SpyObj<WindowLocationService>

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LandingPageComponent],
      imports: [HttpClientModule],
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
          ]),
        },
        {
          provide: WindowLocationService,
          useValue: jasmine.createSpyObj('WindowLocationService', ['updateWindowLocation', 'getWindowLocationHash']),
        },
      ],
    })
    fixture = TestBed.createComponent(LandingPageComponent)
    component = fixture.componentInstance
    landingPageService = TestBed.inject(LandingPageService) as jasmine.SpyObj<LandingPageService>
    windowLocationService = TestBed.inject(WindowLocationService) as jasmine.SpyObj<WindowLocationService>
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('New record connection should redirect to the registry', () => {
    windowLocationService.updateWindowLocation.and.returnValue()
    landingPageService.getOrcidConnectionRecord.and.returnValue(of(new OrcidRecord('email', 'orcid')))
    landingPageService.getMemberInfo.and.returnValue(
      of(new Member('id', 'name', 'email', 'orcid', 'salesforceId', 'clientId'))
    )
    component.processRequest('someState', '', '')
    expect(landingPageService.getOrcidConnectionRecord).toHaveBeenCalled()
    expect(component.oauthUrl).toBe(
      'https://localhost.orcid.org/oauth/authorize?response_type=token&redirect_uri=/landing-page&client_id=name&scope=/read-limited /activities/update /person/update openid&prompt=login&state=someState'
    )
    expect(landingPageService.getPublicKey).toHaveBeenCalledTimes(0)
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalled()
  })

  it('New record connection should fail (user denied permission)', () => {
    windowLocationService.updateWindowLocation.and.returnValue()
    windowLocationService.getWindowLocationHash.and.returnValue('#error=access_denied')
    landingPageService.getOrcidConnectionRecord.and.returnValue(of(new OrcidRecord('email', 'orcid')))
    landingPageService.getMemberInfo.and.returnValue(
      of(new Member('id', 'name', 'email', 'orcid', 'salesforceId', 'clientId'))
    )
    landingPageService.submitUserResponse.and.returnValue(of(''))
    component.processRequest('someState', '', '')
    expect(landingPageService.getOrcidConnectionRecord).toHaveBeenCalled()
    expect(component.oauthUrl).toBe(
      'https://localhost.orcid.org/oauth/authorize?response_type=token&redirect_uri=/landing-page&client_id=name&scope=/read-limited /activities/update /person/update openid&prompt=login&state=someState'
    )
    expect(landingPageService.getPublicKey).toHaveBeenCalledTimes(0)
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalledTimes(0)
    expect(landingPageService.submitUserResponse).toHaveBeenCalled()
    expect(component.showError).toBeFalsy()
    expect(component.showDenied).toBeTruthy()
  })

  it('New record connection should fail (generic error)', () => {
    windowLocationService.updateWindowLocation.and.returnValue()
    windowLocationService.getWindowLocationHash.and.returnValue('#error=123')
    landingPageService.getOrcidConnectionRecord.and.returnValue(of(new OrcidRecord('email', 'orcid')))
    landingPageService.getMemberInfo.and.returnValue(
      of(new Member('id', 'name', 'email', 'orcid', 'salesforceId', 'clientId'))
    )
    landingPageService.submitUserResponse.and.returnValue(of(''))
    component.processRequest('someState', '', '')
    expect(landingPageService.getOrcidConnectionRecord).toHaveBeenCalled()
    expect(component.oauthUrl).toBe(
      'https://localhost.orcid.org/oauth/authorize?response_type=token&redirect_uri=/landing-page&client_id=name&scope=/read-limited /activities/update /person/update openid&prompt=login&state=someState'
    )
    expect(landingPageService.getPublicKey).toHaveBeenCalledTimes(0)
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalledTimes(0)
    expect(landingPageService.submitUserResponse).toHaveBeenCalledTimes(0)
    expect(component.showError).toBeTruthy()
    expect(component.showDenied).toBeFalsy()
  })

  it('Existing record connection should be identified', () => {
    windowLocationService.updateWindowLocation.and.returnValue()
    landingPageService.getOrcidConnectionRecord.and.returnValue(of(new OrcidRecord('email', 'orcid')))
    landingPageService.getMemberInfo.and.returnValue(
      of(new Member('id', 'name', 'email', 'orcid', 'salesforceId', 'clientId'))
    )
    landingPageService.getPublicKey.and.returnValue(of(['publicKey']))
    landingPageService.submitUserResponse.and.returnValue(of(''))
    landingPageService.getUserInfo.and.returnValue(of({ givenName: 'givenName', familyName: 'familyName' }))
    spyOn(KEYUTIL.KEYUTIL, 'getKey').and.returnValue(new KEYUTIL.RSAKey())
    spyOn(KEYUTIL.KJUR.jws.JWS, 'verifyJWT').and.returnValue(true)

    component.processRequest('someState', 'it_token', '')

    expect(landingPageService.getOrcidConnectionRecord).toHaveBeenCalled()
    expect(landingPageService.getPublicKey).toHaveBeenCalled()
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalledTimes(0)
  })

  it('Check for wrong user', () => {
    landingPageService.submitUserResponse.and.returnValue(of({ isDifferentUser: true }))
    landingPageService.getPublicKey.and.returnValue(of(['publicKey']))
    landingPageService.getUserInfo.and.returnValue(of({ givenName: 'givenName', familyName: 'familyName' }))
    spyOn(KEYUTIL.KEYUTIL, 'getKey').and.returnValue(new KEYUTIL.RSAKey())
    spyOn(KEYUTIL.KJUR.jws.JWS, 'verifyJWT').and.returnValue(true)
    component.checkSubmitToken('token', 'state', 'access_token')
    expect(landingPageService.submitUserResponse).toHaveBeenCalled()
    expect(component.showConnectionExists).toBeFalsy()
    expect(component.showConnectionExistsDifferentUser).toBeTruthy()
  })

  it('Check for existing connection', () => {
    landingPageService.submitUserResponse.and.returnValue(of({ isSameUserThatAlreadyGranted: true }))
    landingPageService.getPublicKey.and.returnValue(of(['publicKey']))
    landingPageService.getUserInfo.and.returnValue(of({ givenName: 'givenName', familyName: 'familyName' }))
    spyOn(KEYUTIL.KEYUTIL, 'getKey').and.returnValue(new KEYUTIL.RSAKey())
    spyOn(KEYUTIL.KJUR.jws.JWS, 'verifyJWT').and.returnValue(true)
    component.checkSubmitToken('token', 'state', 'access_token')
    expect(landingPageService.submitUserResponse).toHaveBeenCalled()
    expect(component.showConnectionExists).toBeTruthy()
    expect(component.showConnectionExistsDifferentUser).toBeFalsy()
  })
})
