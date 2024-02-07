import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing'
import { FormBuilder, ReactiveFormsModule } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { BehaviorSubject, Observable, of } from 'rxjs'
import { UserUpdateComponent } from './user-update.component'
import { UserService } from './service/user.service'
import { AccountService } from '../account'
import { MemberService } from '../member/service/member.service'
import { AlertService } from '../shared/service/alert.service'
import { ErrorService } from '../error/service/error.service'
import { IUser, User } from './model/user.model'
import { Member } from '../member/model/member.model'
import { UserValidation } from './model/user-validation.model'
import { RouterTestingModule } from '@angular/router/testing'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core'
import { AppComponent } from '../app.component'
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap'

describe('UserUpdateComponent', () => {
  let component: UserUpdateComponent
  let fixture: ComponentFixture<UserUpdateComponent>
  let userService: jasmine.SpyObj<UserService>
  let accountService: jasmine.SpyObj<AccountService>
  let alertService: jasmine.SpyObj<AlertService>
  let memberService: jasmine.SpyObj<MemberService>
  let router: jasmine.SpyObj<Router>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>

  beforeEach(() => {
    const userServiceSpy = jasmine.createSpyObj('UserService', [
      'validate',
      'update',
      'sendActivate',
      'hasOwner',
      'create',
      'update',
    ])
    const accountServiceSpy = jasmine.createSpyObj('AccountService', [
      'getAccountData',
      'hasAnyAuthority',
      'getSalesforceId',
    ])
    const alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast'])
    const memberServiceSpy = jasmine.createSpyObj('MemberService', ['find'])
    const routerSpy = jasmine.createSpyObj('Router', ['navigate'])

    TestBed.configureTestingModule({
      declarations: [UserUpdateComponent],
      imports: [ReactiveFormsModule, RouterTestingModule.withRoutes([]), FontAwesomeModule],
      providers: [
        FormBuilder,
        { provide: UserService, useValue: userServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
        { provide: ErrorService, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents()

    fixture = TestBed.createComponent(UserUpdateComponent)
    component = fixture.componentInstance
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
    memberService = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>

    accountService.getAccountData.and.returnValue(
      of({
        id: 'test',
        activated: true,
        authorities: ['ROLE_USER'],
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
    memberService.find.and.returnValue(of(new Member()))
    userService.validate.and.returnValue(of(new UserValidation(true, null)))
    userService.update.and.returnValue(of({}))
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true))
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should navigate to users list', () => {
    component.navigateToUsersList()
    expect(router.navigate).toHaveBeenCalledWith(['/users'])
  })

  it('should disable salesforceId dropdown on init for non-admin users', () => {
    activatedRoute.data = of({ user: { salesforceId: 'test', id: 'id' } as IUser })
    accountService.hasAnyAuthority.and.returnValue(false)
    fixture.detectChanges()

    expect(component.disableSalesForceIdDD()).toBe(true)
  })

  it('should enable salesforceId dropdown for admin users', () => {
    accountService.hasAnyAuthority.and.returnValue(true)
    fixture.detectChanges()

    expect(component.disableSalesForceIdDD()).toBe(false)
  })

  it('should validate non-owners', () => {
    userService.hasOwner.and.returnValue(of(true))
    fixture.detectChanges()

    component.editForm.patchValue({ salesforceId: '123', mainContact: false })
    component.validateOrgOwners()
    expect(component.hasOwner).toBe(false)
    expect(component.editForm.get('salesforceId')?.disabled).toBe(false)
  })

  it('should validate org owners', () => {
    userService.hasOwner.and.returnValue(of(true))
    fixture.detectChanges()

    component.editForm.patchValue({ salesforceId: '123', mainContact: true })
    component.validateOrgOwners()
    expect(component.hasOwner).toBe(true)
    expect(component.editForm.get('salesforceId')?.disabled).toBe(true)
  })

  it('should create new user', () => {
    component.editForm.patchValue({
      salesforceId: 'sfid',
      email: 'test@test.com',
      firstName: 'firstName',
      lastName: 'lastName',
      activated: false,
      mainContact: false,
    })

    userService.create.and.returnValue(of(new User()))

    component.save()

    expect(userService.validate).toHaveBeenCalled()
    expect(userService.create).toHaveBeenCalled()
    expect(userService.update).toHaveBeenCalledTimes(0)
    expect(router.navigate).toHaveBeenCalledWith(['/users'])
  })

  it('should create new user as org owner', () => {
    component.editForm.patchValue({
      salesforceId: 'sfid',
      email: 'test@test.com',
      firstName: 'firstName',
      lastName: 'lastName',
      activated: false,
      mainContact: true,
    })

    userService.create.and.returnValue(of(new User()))

    component.save()

    expect(userService.validate).toHaveBeenCalled()
    expect(userService.create).toHaveBeenCalled()
    expect(userService.update).toHaveBeenCalledTimes(0)
    expect(router.navigate).toHaveBeenCalledWith(['/'])
  })

  it('should update existing user', () => {
    activatedRoute.data = of({
      user: {
        salesforceId: 'test',
        id: 'test',
        email: 'test@test.com',
        firstName: 'hello',
        lastName: 'hello',
        mainContact: false,
      } as IUser,
    })
    fixture.detectChanges()
    userService.update.and.returnValue(of(new User()))

    component.save()

    expect(userService.validate).toHaveBeenCalled()
    expect(userService.update).toHaveBeenCalled()
    expect(userService.create).toHaveBeenCalledTimes(0)
    expect(router.navigate).toHaveBeenCalledWith(['/users'])
  })

  it('should update user to org owner and redirect to homepage', () => {
    activatedRoute.data = of({
      user: {
        salesforceId: 'test',
        id: 'test',
        email: 'test@test.com',
        firstName: 'hello',
        lastName: 'hello',
        mainContact: true,
      } as IUser,
    })
    fixture.detectChanges()
    userService.update.and.returnValue(of(new User()))

    component.save()

    expect(userService.validate).toHaveBeenCalled()
    expect(userService.update).toHaveBeenCalled()
    expect(userService.create).toHaveBeenCalledTimes(0)
    expect(router.navigate).toHaveBeenCalledWith(['/'])
  })

  it('should update user to org owner and redirect to users list', () => {
    activatedRoute.data = of({
      user: {
        salesforceId: 'test',
        id: 'testing',
        email: 'test@test.com',
        firstName: 'hello',
        lastName: 'hello',
        mainContact: true,
      } as IUser,
    })
    fixture.detectChanges()
    accountService.hasAnyAuthority.and.returnValue(true)
    userService.update.and.returnValue(of(new User()))

    component.save()

    expect(userService.validate).toHaveBeenCalled()
    expect(userService.update).toHaveBeenCalled()
    expect(userService.create).toHaveBeenCalledTimes(0)
    expect(router.navigate).toHaveBeenCalledWith(['/users'])
  })

  it('should send activation email for existing user', () => {
    activatedRoute.data = of({ user: { salesforceId: 'test', id: 'id' } as IUser })
    userService.sendActivate.and.returnValue(of(new User()))
    fixture.detectChanges()

    component.sendActivate()
    expect(userService.sendActivate).toHaveBeenCalled()
  })

  it('should display send activation option for existing user with inactive account', () => {
    component.existentUser = { email: 'test@example.com', activated: false } as IUser
    expect(component.displaySendActivate()).toBe(true)
  })

  it('should not display send activation option for existing user with active account', () => {
    component.existentUser = { email: 'test@example.com', activated: true } as IUser
    expect(component.displaySendActivate()).toBe(false)
  })
})
