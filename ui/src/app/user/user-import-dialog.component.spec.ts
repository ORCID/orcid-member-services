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
import { EMPTY, of } from 'rxjs'

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
    const uploadServiceSpy = jasmine.createSpyObj('FileUploadService', ['uploadFile'])

    TestBed.configureTestingModule({
      declarations: [UserImportDialogComponent],
      imports: [HttpClientTestingModule],
      providers: [
        FormBuilder,
        NgbModal,
        NgbActiveModal,
        { provide: UserService, useValue: userServiceSpy },
        { provide: EventService, useValue: eventServiceSpy },
        { provide: FileUploadService, useValue: uploadServiceSpy },
        { provide: ErrorService, useValue: {} },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(UserImportDialogComponent)
    component = fixture.componentInstance

    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>
    uploadService = TestBed.inject(FileUploadService) as jasmine.SpyObj<FileUploadService>
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should call upload service', () => {
    component.currentFile = getFileList()
    uploadService.uploadFile.and.returnValue(EMPTY)
    component.upload()
    expect(uploadService.uploadFile).toHaveBeenCalled()
  })

  it('errors should be parsed', () => {
    component.currentFile = getFileList()
    uploadService.uploadFile.and.returnValue(
      of('[{"index":1,"message":"A user with email g.nash+575@orcid.org already exists"}]')
    )
    component.upload()
    expect(uploadService.uploadFile).toHaveBeenCalled()
    expect(component.csvErrors.length).toEqual(1)
  })

  const getFileList = () => {
    const blob = new Blob([''], { type: 'text/html' })
    const file = <File>blob
    const fileList: FileList = {
      0: file,
      1: file,
      length: 2,
      item: (index: number) => file,
    }
    return fileList
  }
})
