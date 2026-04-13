import { ComponentFixture, TestBed } from '@angular/core/testing'

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/compiler'
import { FormBuilder } from '@angular/forms'
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { EMPTY, of } from 'rxjs'
import { FileUploadService } from '../shared/service/file-upload.service'
import { MemberImportDialogComponent } from './member-import-dialog.component'

describe('MemberImportDialogComponent', () => {
  let component: MemberImportDialogComponent
  let fixture: ComponentFixture<MemberImportDialogComponent>

  let uploadServiceSpy: jasmine.SpyObj<FileUploadService>

  beforeEach(() => {
    uploadServiceSpy = jasmine.createSpyObj('FileUploadService', ['uploadFile'])

    TestBed.configureTestingModule({
    declarations: [MemberImportDialogComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [],
    providers: [FormBuilder, NgbModal, NgbActiveModal, { provide: FileUploadService, useValue: uploadServiceSpy }, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
})
    fixture = TestBed.createComponent(MemberImportDialogComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    uploadServiceSpy = TestBed.inject(FileUploadService) as jasmine.SpyObj<FileUploadService>
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should call upload service', () => {
    component.currentFile = getFileList()
    uploadServiceSpy.uploadFile.and.returnValue(EMPTY)
    component.upload()
    expect(uploadServiceSpy.uploadFile).toHaveBeenCalled()
  })

  it('errors should be parsed', () => {
    component.currentFile = getFileList()
    uploadServiceSpy.uploadFile.and.returnValue(of('[{"index":1,"message":"error"}]'))
    component.upload()
    expect(uploadServiceSpy.uploadFile).toHaveBeenCalled()
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
    } as unknown as FileList
    return fileList
  }
})
