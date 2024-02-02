import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { UserUpdateComponent } from './user-update.component';
import { UserService } from './service/user.service';
import { AccountService } from '../account';
import { MemberService } from '../member/service/member.service';
import { AlertService } from '../shared/service/alert.service';
import { ErrorService } from '../error/service/error.service';
import { IUser, User } from './model/user.model';
import { IMember } from '../member/model/member.model';
import { UserValidation } from './model/user-validation.model';

describe('UserUpdateComponent', () => {
  let component: UserUpdateComponent;
  let fixture: ComponentFixture<UserUpdateComponent>;
  let userService: jasmine.SpyObj<UserService>;
  let accountService: jasmine.SpyObj<AccountService>;
  let alertService: jasmine.SpyObj<AlertService>;

  beforeEach(() => {
    const userServiceSpy = jasmine.createSpyObj('UserService', [
      'validate',
      'update',
      'sendActivate',
      'hasOwner',
      'create',
      'update'
    ]);
    const accountServiceSpy = jasmine.createSpyObj('AccountService', [
      'getAccountData',
      'hasAnyAuthority',
      'getSalesforceId'
    ]);
    const alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast']);

    TestBed.configureTestingModule({
      declarations: [UserUpdateComponent],
      imports: [ReactiveFormsModule],
      providers: [
        FormBuilder,
        { provide: ActivatedRoute, useValue: { data: of({ user: {} as IUser }) } },
        { provide: Router, useValue: { navigate: () => {} } },
        { provide: UserService, useValue: userServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: MemberService, useValue: {} },
        { provide: AlertService, useValue: alertServiceSpy },
        { provide: ErrorService, useValue: {} },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserUpdateComponent);
    component = fixture.componentInstance;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>;
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>;

    accountService.getAccountData.and.returnValue(of({
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
    }));
    accountService.hasAnyAuthority.and.returnValue(true);
    userService.hasOwner.and.returnValue(of(true));
    userService.validate.and.returnValue(of(new UserValidation(true, null)));
    userService.update.and.returnValue(of({}));
    userService.sendActivate.and.returnValue(of(new User()));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should navigate to users list', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate');
    component.navigateToUsersList();
    expect(navigateSpy).toHaveBeenCalledWith(['/users']);
  });

  it('should disable salesforceId dropdown for non-admin users', () => {
    accountService.hasAnyAuthority.and.returnValue(false);
    expect(component.disableSalesForceIdDD()).toBe(true);
  });

  it('should enable salesforceId dropdown for admin users', () => {
    accountService.hasAnyAuthority.and.returnValue(true);
    expect(component.disableSalesForceIdDD()).toBe(false);
  });

  it('should validate org owners', () => {
    component.editForm.patchValue({ salesforceId: '123', mainContact: true });
    component.validateOrgOwners();
    expect(component.hasOwner).toBe(true);
    expect(component.editForm.get('salesforceId')?.disabled).toBe(true);
  });

  it('should create new user', fakeAsync(() => {
    component.editForm.patchValue({salesforceId: 'test', email: "test@test.com", firstName: "firstName", lastName: "lastName", activated: false,  })
    userService.create.and.returnValue(of(new User()))    
    component.save();
    tick();
    expect(userService.validate).toHaveBeenCalled();
    expect(userService.create).toHaveBeenCalled();
  }));

  it('should send activation email for existing user', fakeAsync(() => {
    component.existentUser = { email: 'test@example.com', activated: false } as IUser;
    component.sendActivate();
    tick();
    expect(userService.sendActivate).toHaveBeenCalled();
  }));

  it('should display send activation option for existing user with unactivated email', () => {
    component.existentUser = { email: 'test@example.com', activated: false } as IUser;
    expect(component.displaySendActivate()).toBe(true);
  });

});
