import { ComponentFixture, TestBed } from '@angular/core/testing'

import { UserDetailComponent } from './user-detail.component'
import { RouterTestingModule } from '@angular/router/testing'
import { HttpClientModule } from '@angular/common/http'
import { AlertService } from '../shared/service/alert.service'
import { UserService } from './service/user.service'
import { MemberService } from '../member/service/member.service'
import { User } from './model/user.model'
import { of } from 'rxjs'

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
      declarations: [UserDetailComponent],
      imports: [RouterTestingModule, HttpClientModule],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
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
    component.user = new User()
    userServiceSpy.sendActivate.and.returnValue(of(new User()))

    component.sendActivate()

    expect(userServiceSpy.sendActivate).toHaveBeenCalled()
    expect(alertServiceSpy.broadcast).toHaveBeenCalled()
  })

  it('when user does not exist, sendActivate should not call userService or alertService', () => {
    component.user = null
    userServiceSpy.sendActivate.and.returnValue(of(new User()))

    component.sendActivate()

    expect(userServiceSpy.sendActivate).toHaveBeenCalledTimes(0)
    expect(alertServiceSpy.broadcast).toHaveBeenCalledTimes(0)
  })
})
