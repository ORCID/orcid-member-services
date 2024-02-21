import { ComponentFixture, TestBed } from '@angular/core/testing'

import { UserImportDialogComponent } from './user-import-dialog.component'
import { FormBuilder, ReactiveFormsModule } from '@angular/forms'
import { RouterTestingModule } from '@angular/router/testing'
import { UserService } from './service/user.service'
import { ErrorService } from '../error/service/error.service'
import { EventService } from '../shared/service/event.service'
import { FileUploadService } from '../shared/service/file-upload.service'
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { HttpClientTestingModule } from '@angular/common/http/testing'

describe('UserImportDialogComponent', () => {
  let component: UserImportDialogComponent
  let fixture: ComponentFixture<UserImportDialogComponent>

  let userService: jasmine.SpyObj<UserService>
  let eventService: jasmine.SpyObj<EventService>
  let uploadService: jasmine.SpyObj<FileUploadService>

  beforeEach(() => {
    const userServiceSpy = jasmine.createSpyObj('UserService', [
      'validate',
      'update',
      'sendActivate',
      'hasOwner',
      'create',
      'update',
    ])
    const eventServiceSpy = jasmine.createSpyObj('EventService', ['broadcast', 'on'])
    const uploadServiceSpy = jasmine.createSpyObj('UploadService', ['uploadFile'])

    TestBed.configureTestingModule({
      declarations: [UserImportDialogComponent],
      imports: [HttpClientTestingModule],
      providers: [
        FormBuilder,
        NgbModal,
        NgbActiveModal,
        { provide: UserService, useValue: userServiceSpy },
        { provide: EventService, useValue: eventServiceSpy },
        { provide: FileUploadService, useValue: uploadService },
        { provide: ErrorService, useValue: {} },
      ],
    })
    fixture = TestBed.createComponent(UserImportDialogComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>
    uploadService = TestBed.inject(FileUploadService) as jasmine.SpyObj<FileUploadService>
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
