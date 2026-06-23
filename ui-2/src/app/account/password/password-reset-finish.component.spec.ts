/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing'

import { ReactiveFormsModule } from '@angular/forms'
import { ActivatedRoute } from '@angular/router'
import { RouterModule } from '@angular/router'
import { of, throwError } from 'rxjs'
import { PasswordService } from '../service/password.service'
import { PasswordResetFinishComponent } from './password-reset-finish.component'

describe('PasswordResetFinishComponent', () => {
  let component: PasswordResetFinishComponent
  let fixture: ComponentFixture<PasswordResetFinishComponent>
  let service: jasmine.SpyObj<PasswordService>

  beforeEach(() => {
    const passwordServiceSpy = jasmine.createSpyObj('PasswordService', [
      'validateKey',
      'savePassword',
      'resendActivationEmail',
    ])
    passwordServiceSpy.validateKey.and.returnValue(of({ invalidKey: false, expiredKey: false }))

    TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, RouterModule.forRoot([]), PasswordResetFinishComponent],
      providers: [
        { provide: PasswordService, useValue: passwordServiceSpy },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
      ],
    })
    fixture = TestBed.createComponent(PasswordResetFinishComponent)
    service = TestBed.inject(PasswordService) as jasmine.SpyObj<PasswordService>
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('password should save successfully', () => {
    service.savePassword.and.returnValue(of(true))
    component.finishReset()
    expect((component as any).success()).toEqual('OK')
  })

  it('password save should fail', () => {
    service.savePassword.and.returnValue(throwError(() => new Error('error')))
    component.finishReset()
    expect((component as any).error()).toEqual('ERROR')
    expect((component as any).success()).toBeFalsy()
  })
})
