import { ComponentFixture, TestBed } from '@angular/core/testing'

import { UserDeleteDialogComponent } from './user-delete.component'
import { RouterTestingModule } from '@angular/router/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { EventService } from '../shared/service/event.service'
import { UserService } from './service/user.service'
import { of } from 'rxjs'
import { EventType } from '../app.constants'
import { Event } from '../shared/model/event.model'
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap'

describe('UserDeleteComponent', () => {
  let userServiceSpy: jasmine.SpyObj<UserService>
  let eventServiceSpy: jasmine.SpyObj<EventService>
  let component: UserDeleteDialogComponent
  let fixture: ComponentFixture<UserDeleteDialogComponent>

  beforeEach(() => {
    userServiceSpy = jasmine.createSpyObj('UserService', ['delete'])
    eventServiceSpy = jasmine.createSpyObj('EventService', ['broadcast'])
    TestBed.configureTestingModule({
      declarations: [UserDeleteDialogComponent],
      imports: [RouterTestingModule],
      providers: [
        NgbModal,
        NgbActiveModal,
        { provide: UserService, useValue: userServiceSpy },
        { provide: EventService, useValue: eventServiceSpy },
      ],

      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })

    userServiceSpy = TestBed.inject(UserService) as jasmine.SpyObj<UserService>
    eventServiceSpy = TestBed.inject(EventService) as jasmine.SpyObj<EventService>
    fixture = TestBed.createComponent(UserDeleteDialogComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should confirm the user being deleted', () => {
    userServiceSpy.delete.and.returnValue(of(true))
    component.confirmDelete('id')

    expect(eventServiceSpy.broadcast).toHaveBeenCalledWith(new Event(EventType.USER_LIST_MODIFIED, 'Deleted a user'))
  })

  it('should not call the userservice without a provided id', () => {
    userServiceSpy.delete.and.returnValue(of(false))
    component.confirmDelete('')

    expect(userServiceSpy.delete).toHaveBeenCalledTimes(0)
  })
})
