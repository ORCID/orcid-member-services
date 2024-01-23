import { ComponentFixture, TestBed, tick, fakeAsync } from '@angular/core/testing'
import { UsersComponent } from './users.component'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { RouterTestingModule } from '@angular/router/testing'
import { EMPTY, of, throwError } from 'rxjs'
import { MemberService } from 'src/app/member/service/member.service'
import { AccountService, LoginService } from 'src/app/account'
import { By } from '@angular/platform-browser'
import { HasAnyAuthorityDirective } from 'src/app/shared/directive/has-any-authority.directive'
import { HttpHeaders, HttpResponse } from '@angular/common/http'
import { Member } from 'src/app/member/model/member.model'
import { UserService } from './service/user.service'
import { UserPage } from './model/user-page.model'
import { User } from './model/user.model'
import { AlertService } from '../shared/service/alert.service'
import { EventService } from '../shared/service/event.service'
import { LocalizePipe } from '../shared/pipe/localize'
import { EventType } from 'src/app/app.constants'
import { Event } from '../shared/model/event.model'
describe('UsersComponent', () => {
  let component: UsersComponent
  let fixture: ComponentFixture<UsersComponent>
  let userService: jasmine.SpyObj<UserService>
  let accountService: jasmine.SpyObj<AccountService>
  let alertService: jasmine.SpyObj<AlertService>
  let eventService: jasmine.SpyObj<EventService>

  beforeEach(() => {
    const accountServiceSpy = jasmine.createSpyObj('AccountService', [
      'getAccountData',
      'isAuthenticated',
      'hasAnyAuthority',
      'isLoggedAs',
      'isOrganizationOwner',
      'getImageUrl',
      'getSalesforceId',
    ])
    const userServiceSpy = jasmine.createSpyObj('UserService', [
      'query',
      'findBySalesForceId',
      'sendActivate',
      'switchUser',
    ])
    const eventServiceSpy = jasmine.createSpyObj('EventService', ['on', 'broadcast'])
    const alertServiceSpy = jasmine.createSpyObj('AlertService', ['on', 'broadcast'])

    TestBed.configureTestingModule({
      declarations: [UsersComponent, HasAnyAuthorityDirective, LocalizePipe],
      imports: [ReactiveFormsModule, RouterTestingModule, FormsModule],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: EventService, useValue: eventServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(UsersComponent)
    component = fixture.componentInstance
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
  })

  fit('should create', () => {
    expect(component).toBeTruthy()
  })

  fit('should call load all on init', fakeAsync(() => {
    const headers = new HttpHeaders().append('link', 'link;link')
    userService.query.and.returnValue(of(new UserPage([new User('123')], 1)))
    accountService.hasAnyAuthority.and.returnValue(true)
    eventService.on.and.returnValue(EMPTY)
    accountService.getAccountData.and.returnValue(
      of({
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
      })
    )
    fixture.detectChanges()
    tick()
    component.ngOnInit()

    expect(userService.query).toHaveBeenCalled()
    expect(component.users![0]).toEqual(jasmine.objectContaining({ id: '123' }))
  }))
})
