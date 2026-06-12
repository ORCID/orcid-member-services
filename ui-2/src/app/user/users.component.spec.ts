import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing'
import { By } from '@angular/platform-browser'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'
import { EMPTY, of } from 'rxjs'
import { AccountService } from 'src/app/account'
import { HasAnyAuthorityDirective } from 'src/app/shared/directive/has-any-authority.directive'
import { LocalizePipe } from '../shared/pipe/localize'
import { AlertService } from '../shared/service/alert.service'
import { EventService } from '../shared/service/event.service'
import { User } from './model/user.model'
import { UserService } from './service/user.service'
import { UsersComponent } from './users.component'
import { FeatureToggleService } from '../shared/service/feature-toggle.service'
describe('UsersComponent', () => {
  let component: UsersComponent
  let fixture: ComponentFixture<UsersComponent>
  let userService: jasmine.SpyObj<UserService>
  let accountService: jasmine.SpyObj<AccountService>
  let alertService: jasmine.SpyObj<AlertService>
  let eventService: jasmine.SpyObj<EventService>
  let featureToggleService: jasmine.SpyObj<FeatureToggleService>

  beforeEach(() => {
    const accountServiceSpy = jasmine.createSpyObj('AccountService', [
      'getAccountData',
      'isAuthenticated',
      'hasAnyAuthority',
      'isOrganizationOwner',
      'getImageUrl',
      'getSalesforceId',
      'getMemberId',
    ])
    const userServiceSpy = jasmine.createSpyObj('UserService', ['query', 'findBySalesForceId', 'sendActivate', 'findByMemberId'])
    const eventServiceSpy = jasmine.createSpyObj('EventService', ['on', 'broadcast'])
    const alertServiceSpy = jasmine.createSpyObj('AlertService', ['on', 'broadcast'])
    const featureToggleSpy = jasmine.createSpyObj('FeatureToggleService', ['isEnabled', 'initFeatures']);
    featureToggleSpy.initFeatures.and.returnValue(of(null));

    TestBed.configureTestingModule({
      declarations: [UsersComponent, HasAnyAuthorityDirective, LocalizePipe],
      imports: [
        ReactiveFormsModule,
        RouterTestingModule,
        RouterModule.forChild([{ path: 'users', component: UsersComponent }]),
        FormsModule,
      ],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: EventService, useValue: eventServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
        { provide: FeatureToggleService, useValue: featureToggleSpy }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents()

    fixture = TestBed.createComponent(UsersComponent)
    component = fixture.componentInstance
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
    featureToggleService = TestBed.inject(FeatureToggleService) as jasmine.SpyObj<FeatureToggleService>

    const usersResponse = of({
      content: [new User('123')], // Or just { id: '123' } as User if you switched to interfaces
      page: {
        totalElements: 1,
        number: 0,
        size: 20,
        totalPages: 1,
      },
    })
    userService.query.and.returnValue(usersResponse)
    userService.findByMemberId.and.returnValue(usersResponse)

    accountService.getAccountData.and.returnValue(
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

  it('sort should be id,asc by default', () => {
    const result = component.sort()
    expect(result).toEqual(['id,asc'])
  })

  it('direction should be asc and id should be secondary sort column by default', () => {
    component.sortColumn = 'name'
    const result = component.sort()
    expect(result).toEqual(['name,asc', 'id'])
  })

  it('updating sort column to different value should maintain sort direction', () => {
    component.sortColumn = 'name'
    let result = component.sort()
    expect(result).toEqual(['name,asc', 'id'])

    component.updateSort('email')
    result = component.sort()
    expect(result).toEqual(['email,asc', 'id'])
  })

  it('updating sort column with same value should flip sort direction', () => {
    component.sortColumn = 'name'
    let result = component.sort()
    expect(result).toEqual(['name,asc', 'id'])

    component.updateSort('name')
    result = component.sort()
    expect(result).toEqual(['name,desc', 'id'])
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

  it('2FA column should be visible for admin users', () => {
    featureToggleService.isEnabled.withArgs('MANAGE_API_CREDENTIALS').and.returnValue(true);

    accountService.hasAnyAuthority.and.returnValue(true)
    component.ngOnInit()
    fixture.detectChanges()

    const twoFaHeaders = fixture.debugElement
      .queryAll(By.css('th'))
      .filter((el) => el.nativeElement.textContent.includes('2FA'))
    expect(twoFaHeaders.length).toEqual(1)
  })

  it('2FA column should not be visible for non-admin users', () => {
    accountService.hasAnyAuthority.and.returnValue(false)
    component.ngOnInit()
    fixture.detectChanges()

    const twoFaHeaders = fixture.debugElement
      .queryAll(By.css('th'))
      .filter((el) => el.nativeElement.textContent.includes('2FA'))
    expect(twoFaHeaders.length).toEqual(0)
  })
})
