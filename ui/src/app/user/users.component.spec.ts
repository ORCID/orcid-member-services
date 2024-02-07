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
import { RouterModule } from '@angular/router'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
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
      imports: [ReactiveFormsModule, RouterModule.forRoot([{ path: 'users', component: UsersComponent }]), FormsModule],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: EventService, useValue: eventServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents()

    fixture = TestBed.createComponent(UsersComponent)
    component = fixture.componentInstance
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>

    userService.query.and.returnValue(of(new UserPage([new User('123')], 1)))

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

    accountService.hasAnyAuthority.and.returnValue(true)
    eventService.on.and.returnValue(EMPTY)
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should call load all on init', fakeAsync(() => {
    component.ngOnInit()

    expect(userService.query).toHaveBeenCalled()
    expect(component.users![0]).toEqual(jasmine.objectContaining({ id: '123' }))
  }))

  it('should load a page', () => {
    component.page = 1
    component.loadPage()

    expect(userService.query).toHaveBeenCalled()
    expect(component.users![0]).toEqual(jasmine.objectContaining({ id: '123' }))
  })

  it('sort should be id,desc by default', () => {
    const result = component.sort()
    expect(result).toEqual(['id,desc'])
  })

  it('direction should be desc and id should be secondary sort column by default', () => {
    component.sortColumn = 'name'
    const result = component.sort()
    expect(result).toEqual(['name,desc', 'id'])
  })

  it('updating sort column to different value should maintain sort direction', () => {
    component.sortColumn = 'name'
    let result = component.sort()
    expect(result).toEqual(['name,desc', 'id'])

    component.updateSort('email')
    result = component.sort()
    expect(result).toEqual(['email,desc', 'id'])
  })

  it('updating sort column with same value should flip sort direction', () => {
    component.sortColumn = 'name'
    let result = component.sort()
    expect(result).toEqual(['name,desc', 'id'])

    component.updateSort('name')
    result = component.sort()
    expect(result).toEqual(['name,asc', 'id'])
  })

  it('clear should reset page to zero', () => {
    component.page = 10
    component.clear()
    expect(component.page).toEqual(0)
  })

  it('reset search should clear search term', () => {
    component.searchTerm = 'what the user typed'
    component.submittedSearchTerm = 'what the user typed'
    component.resetSearch()
    expect(component.searchTerm).toEqual('')
    expect(component.submittedSearchTerm).toEqual('')
  })
})
