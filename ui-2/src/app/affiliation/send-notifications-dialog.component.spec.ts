import { ComponentFixture, TestBed } from '@angular/core/testing'

import { SendNotificationsDialogComponent } from './send-notifications-dialog.component'
import { NotificationService } from './service/notification.service'
import { EventService } from '../shared/service/event.service'
import { AccountService } from '../account'
import { MemberService } from '../member/service/member.service'
import { AlertService } from '../shared/service/alert.service'
import { LanguageService } from '../shared/service/language.service'
import { HttpClientTestingModule } from '@angular/common/http/testing'
import { FormBuilder } from '@angular/forms'
import { NgbModal, NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { ErrorService } from '../error/service/error.service'
import { FileUploadService } from '../shared/service/file-upload.service'
import { UserService } from '../user/service/user.service'
import { of } from 'rxjs'

describe('SendNotificationsDialogComponent', () => {
  let component: SendNotificationsDialogComponent
  let fixture: ComponentFixture<SendNotificationsDialogComponent>

  let notificationServiceSpy: jasmine.SpyObj<NotificationService>
  let eventServiceSpy: jasmine.SpyObj<EventService>
  let alertServiceSpy: jasmine.SpyObj<AlertService>
  let languageServiceSpy: jasmine.SpyObj<LanguageService>
  let memberServiceSpy: jasmine.SpyObj<MemberService>
  let accountServiceSpy: jasmine.SpyObj<AccountService>

  beforeEach(() => {
    eventServiceSpy = jasmine.createSpyObj('EventService', ['broadcast', 'on'])
    alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast', 'on'])
    memberServiceSpy = jasmine.createSpyObj('MemberService', ['find'])
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['updateStatuses', 'requestInProgress'])
    languageServiceSpy = jasmine.createSpyObj('LanguageService', [
      'getAllLanguages',
      'getCurrentLanguage',
      'changeLanguage',
    ])

    TestBed.configureTestingModule({
      declarations: [SendNotificationsDialogComponent],
      imports: [HttpClientTestingModule],
      providers: [
        FormBuilder,
        NgbModal,
        NgbActiveModal,
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: EventService, useValue: eventServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: LanguageService, useValue: languageServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: ErrorService, useValue: {} },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(SendNotificationsDialogComponent)
    component = fixture.componentInstance

    eventServiceSpy = TestBed.inject(EventService) as jasmine.SpyObj<EventService>
    accountServiceSpy = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    alertServiceSpy = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
    languageServiceSpy = TestBed.inject(LanguageService) as jasmine.SpyObj<LanguageService>
    memberServiceSpy = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
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
      })
    )

    expect(component).toBeTruthy()
  })

  it('send should call notification service', () => {
    notificationServiceSpy.requestInProgress.and.returnValue(of({ inProgress: false }))
    notificationServiceSpy.updateStatuses.and.returnValue(of({}))
    component.send()
    expect(notificationServiceSpy.updateStatuses).toHaveBeenCalled()
  })

  it('send should not call notification service when request is already in progress', () => {
    notificationServiceSpy.requestInProgress.and.returnValue(of({ inProgress: true }))
    component.send()
    expect(notificationServiceSpy.updateStatuses).toHaveBeenCalledTimes(0)
  })
})
