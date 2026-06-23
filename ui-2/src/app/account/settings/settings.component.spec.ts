import { ComponentFixture, TestBed } from '@angular/core/testing'

import { SettingsComponent } from './settings.component'
import { ReactiveFormsModule } from '@angular/forms'
import { HttpResponse, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'
import { LanguageService } from 'src/app/shared/service/language.service'
import { AccountService } from '../service/account.service'
import { of } from 'rxjs'
import { FindLanguageFromKeyPipe } from 'src/app/shared/pipe/find-language-from-key'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'

describe('SettingsComponent', () => {
  let component: SettingsComponent
  let fixture: ComponentFixture<SettingsComponent>
  let accountServiceSpy: jasmine.SpyObj<AccountService>
  let languageServiceSpy: jasmine.SpyObj<LanguageService>

  beforeEach(() => {
    accountServiceSpy = jasmine.createSpyObj('AccountService', [
      'getAccountData',
      'getUsername',
      'save',
      'getMfaSetup',
      'enableMfa',
      'disableMfa',
    ])
    languageServiceSpy = jasmine.createSpyObj('LanguageService', [
      'getAllLanguages',
      'getCurrentLanguage',
      'changeLanguage',
    ])

    TestBed.configureTestingModule({
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      imports: [ReactiveFormsModule, SettingsComponent, FindLanguageFromKeyPipe],
      providers: [
        { provide: LanguageService, useValue: languageServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        provideHttpClient(withInterceptorsFromDi()),
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(SettingsComponent)
    component = fixture.componentInstance
    accountServiceSpy = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>

    languageServiceSpy.getAllLanguages.and.returnValue({
      en: { name: 'English' },
      es: { name: 'Español' },
      fr: { name: 'Français' },
      ja: { name: '日本語' },
      'zh-TW': { name: '繁體中文' },
      'zh-CN': { name: '简体中文' },
      cs: { name: 'Čeština' },
      it: { name: 'Italiano' },
      ko: { name: '한국어' },
      pt: { name: 'Português' },
      ru: { name: 'Pусский' },
      xx: { name: 'Test' },
    })
  })

  it('should create', () => {
    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['test', 'test'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        loggedAs: false,
        loginAs: 'sfid',
        mainContact: false,
        mfaEnabled: true,
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )

    expect(component).toBeTruthy()
  })

  it('should flip mfa fields when mfa state changed', () => {
    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['test', 'test'],
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
    accountServiceSpy.getMfaSetup.and.returnValue(of({ secret: 'test', otp: 'test', qrCode: 'test' }))
    accountServiceSpy.getUsername.and.returnValue('test')
    fixture.detectChanges()

    expect((component as any).showMfaSetup()).toBeFalsy()
    expect((component as any).showMfaBackupCodes()).toBeFalsy()

    component.mfaForm.patchValue({ mfaEnabled: true })
    component.mfaEnabledStateChange()

    expect((component as any).showMfaSetup()).toBeTruthy()
    expect((component as any).showMfaBackupCodes()).toBeFalsy()
  })

  it('should flip mfa fields when mfa state changed', () => {
    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['test', 'test'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        loggedAs: false,
        loginAs: 'sfid',
        mainContact: false,
        mfaEnabled: true,
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )
    accountServiceSpy.getMfaSetup.and.returnValue(of({ secret: 'test', otp: 'test', qrCode: 'test' }))

    expect((component as any).showMfaTextCode()).toBeFalsy()

    component.toggleMfaTextCode()

    expect((component as any).showMfaTextCode()).toBeTruthy()
  })

  it('save mfa enabled should call account service enable', () => {
    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['test', 'test'],
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
    accountServiceSpy.getMfaSetup.and.returnValue(of({ secret: 'test', otp: 'test', qrCode: ['test'] }))
    accountServiceSpy.enableMfa.and.returnValue(of(['code1', 'code2']))
    fixture.detectChanges()

    component.mfaForm.patchValue({ mfaEnabled: true, verificationCode: 'test' })
    component.saveMfa()

    expect(accountServiceSpy.enableMfa).toHaveBeenCalled()
    expect(accountServiceSpy.disableMfa).toHaveBeenCalledTimes(0)
  })

  it('save mfa enabled should call account service disable', () => {
    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['test', 'test'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        loggedAs: false,
        loginAs: 'sfid',
        mainContact: false,
        mfaEnabled: true,
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )
    accountServiceSpy.getMfaSetup.and.returnValue(of({ secret: 'test', otp: 'test', qrCode: ['test'] }))
    accountServiceSpy.disableMfa.and.returnValue(of(true))
    fixture.detectChanges()

    component.mfaForm.patchValue({ mfaEnabled: false, verificationCode: 'test' })
    component.saveMfa()

    expect(accountServiceSpy.disableMfa).toHaveBeenCalled()
    expect(accountServiceSpy.enableMfa).toHaveBeenCalledTimes(0)
  })

  it('save form should call accountService.save and then account data requested when save is successful', () => {
    accountServiceSpy.save.and.returnValue(of(true))
    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['test', 'test'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        loggedAs: false,
        loginAs: 'sfid',
        mainContact: false,
        mfaEnabled: true,
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )
    fixture.detectChanges()
    expect((component as any).success()).toBeFalsy()
    component.save()
    expect((component as any).success()).toBeTruthy()
    expect(accountServiceSpy.save).toHaveBeenCalled()
  })

  it('save form should call accountService.save and then account data requested when save is successful', () => {
    accountServiceSpy.save.and.returnValue(of(false))
    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['test', 'test'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        loggedAs: false,
        loginAs: 'sfid',
        mainContact: false,
        mfaEnabled: true,
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )
    fixture.detectChanges()
    expect((component as any).success()).toBeFalsy()
    component.save()
    expect((component as any).success()).toBeFalsy()
    expect(accountServiceSpy.save).toHaveBeenCalled()
  })
})
