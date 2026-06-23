import { ComponentFixture, TestBed } from '@angular/core/testing'

import { UserDetailComponent } from './user-detail.component'
import { RouterModule } from '@angular/router'
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'
import { WritableSignal } from '@angular/core'
import { AlertService } from '../shared/service/alert.service'
import { UserService } from './service/user.service'
import { MemberService } from '../member/service/member.service'
import { IUser, User } from './model/user.model'
import { of } from 'rxjs'

type UserDetailInternals = {
  user: WritableSignal<IUser | null>
}
const internals = (component: UserDetailComponent): UserDetailInternals =>
  component as unknown as UserDetailInternals

describe('UserDetailComponent', () => {
  let component: UserDetailComponent
  let fixture: ComponentFixture<UserDetailComponent>
  let userServiceSpy: jasmine.SpyObj<UserService>
  let alertServiceSpy: jasmine.SpyObj<AlertService>
  let memberServiceSpy: jasmine.SpyObj<MemberService>

  beforeEach(() => {
    memberServiceSpy = jasmine.createSpyObj('MemberService', ['find'])
    userServiceSpy = jasmine.createSpyObj('UserService', ['find', 'sendActivate'])
    alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast'])

    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), UserDetailComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
        provideHttpClient(withInterceptorsFromDi()),
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(UserDetailComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    userServiceSpy = TestBed.inject(UserService) as jasmine.SpyObj<UserService>
    memberServiceSpy = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    alertServiceSpy = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('when user exists, sendActivate should call userService and alertService', () => {
    internals(component).user.set(new User())
    userServiceSpy.sendActivate.and.returnValue(of(new User()))

    component.sendActivate()

    expect(userServiceSpy.sendActivate).toHaveBeenCalled()
    expect(alertServiceSpy.broadcast).toHaveBeenCalled()
  })

  it('when user does not exist, sendActivate should not call userService or alertService', () => {
    internals(component).user.set(null)
    userServiceSpy.sendActivate.and.returnValue(of(new User()))

    component.sendActivate()

    expect(userServiceSpy.sendActivate).toHaveBeenCalledTimes(0)
    expect(alertServiceSpy.broadcast).toHaveBeenCalledTimes(0)
  })
})
