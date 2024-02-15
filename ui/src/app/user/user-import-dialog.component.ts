import { Component, OnDestroy, OnInit } from '@angular/core'
import { FileUploadService } from '../shared/service/file-upload.service'
import { IUser } from './model/user.model'
import { UserService } from './service/user.service'
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'
import { EventService } from '../shared/service/event.service'
import { Event } from '../shared/model/event.model'
import { EventType } from '../app.constants'
import { ActivatedRoute, Router } from '@angular/router'
import { faSave, faBan } from '@fortawesome/free-solid-svg-icons'

@Component({
  selector: 'app-user-import-dialog',
  templateUrl: './user-import-dialog.component.html',
  styleUrls: ['./user-import-dialog.component.scss'],
  providers: [FileUploadService],
})
export class UserImportDialogComponent {
  resourceUrl: string
  user: IUser | null
  isSaving: boolean
  currentFile: FileList | null
  csvErrors: any
  loading = false
  faBan = faBan
  faSave = faSave

  constructor(
    protected userService: UserService,
    public activeModal: NgbActiveModal,
    protected eventService: EventService,
    private uploadService: FileUploadService
  ) {
    this.isSaving = false
    this.user = null
    this.currentFile = null
    this.resourceUrl = this.userService.resourceUrl + '/upload'
  }

  clear() {
    this.activeModal.dismiss('cancel')
  }

  selectFile(event: any) {
    this.currentFile = event.target.files
  }

  upload() {
    console.log('resource url is ', this.resourceUrl)
    if (this.currentFile) {
      console.log('found current file')
      this.loading = true
      const f = this.currentFile.item(0)
      this.uploadService.uploadFile(this.resourceUrl, f!, 'text').subscribe((res: string) => {
        console.log('got result ', res)
        console.log('parsing errors')
        this.csvErrors = JSON.parse(res)
        console.log('parsed errors')
        this.loading = false
        if (this.csvErrors.size === 0) {
          this.eventService.broadcast(new Event(EventType.USER_LIST_MODIFIED, 'New user settings uploaded'))
          console.log('dismissing dialog')
          this.activeModal.dismiss(true)
        }
      })
    } else {
      alert(
        $localize`:gatewayApp.msUserServiceMSUser.import.emptyFile.string:There is no file to upload. Please select one.`
      )
    }
  }
}

@Component({
  selector: 'app-user-import-popup',
  template: '',
})
export class UserImportPopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef | undefined | null

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal
  ) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ u }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(UserImportDialogComponent as Component, {
          size: 'lg',
          backdrop: 'static',
        })
        this.ngbModalRef.componentInstance.user = u
        this.ngbModalRef.result.then(
          (result) => {
            this.router.navigate(['/users', { outlets: { popup: null } }])
            this.ngbModalRef = null
          },
          (reason) => {
            this.router.navigate(['/users', { outlets: { popup: null } }])
            this.ngbModalRef = null
          }
        )
      }, 0)
    })
  }

  ngOnDestroy() {
    this.ngbModalRef = null
    this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => this.router.navigate(['/users']))
  }
}
