import { ComponentFixture, TestBed } from '@angular/core/testing'

import { AffiliationImportDialogComponent } from './affiliation-import-dialog.component'
import { AffiliationService } from './service/affiliation.service'
import { EventService } from '../shared/service/event.service'
import { FileUploadService } from '../shared/service/file-upload.service'
import { HttpClientTestingModule } from '@angular/common/http/testing'
import { FormBuilder } from '@angular/forms'
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { EMPTY, of } from 'rxjs'

describe('AffiliationImportDialogComponent', () => {
  let component: AffiliationImportDialogComponent
  let fixture: ComponentFixture<AffiliationImportDialogComponent>
  let eventService: jasmine.SpyObj<EventService>
  let uploadService: jasmine.SpyObj<FileUploadService>

  beforeEach(() => {
    const eventServiceSpy = jasmine.createSpyObj('EventService', ['broadcast', 'on'])
    const uploadServiceSpy = jasmine.createSpyObj('FileUploadService', ['uploadFile'])

    TestBed.configureTestingModule({
      declarations: [AffiliationImportDialogComponent],
      imports: [HttpClientTestingModule],
      providers: [
        FormBuilder,
        NgbModal,
        NgbActiveModal,
        { provide: EventService, useValue: eventServiceSpy },
        { provide: FileUploadService, useValue: uploadServiceSpy },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(AffiliationImportDialogComponent)
    component = fixture.componentInstance

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
