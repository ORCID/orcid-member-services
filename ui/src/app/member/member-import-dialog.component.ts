import { Component, OnDestroy, OnInit } from '@angular/core'
import { MemberService } from './service/member.service'
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'
import { EventService } from '../shared/service/event.service'
import { FileUploadService } from '../shared/service/file-upload.service'
import { ActivatedRoute, Router } from '@angular/router'
import { Event } from '../shared/model/event.model'
import { EventType } from '../app.constants'
import { faBan, faSave } from '@fortawesome/free-solid-svg-icons'

@Component({
  selector: 'app-member-import-dialog',
  templateUrl: './member-import-dialog.component.html',
  styleUrls: ['./member-import-dialog.component.scss'],
})
export class MemberImportDialogComponent {
  public resourceUrl
  isSaving: boolean
  currentFile: FileList | null
  csvErrors: any
  loading = false
  faBan = faBan
  faSave = faSave

  constructor(
    protected memberService: MemberService,
    public activeModal: NgbActiveModal,
    protected eventService: EventService,
    private uploadService: FileUploadService
  ) {
    this.currentFile = null
    this.isSaving = false
    this.resourceUrl = this.memberService.resourceUrl + '/members/upload'
  }

  clear() {
    this.activeModal.dismiss('cancel')
  }

  selectFile(event: any) {
    this.currentFile = event.target.files
  }

  upload() {
    if (this.currentFile) {
      this.loading = true
      const f = this.currentFile.item(0)
      this.uploadService.uploadFile(this.resourceUrl, f!, 'text').subscribe((res: any) => {
        if (res) {
          this.csvErrors = JSON.parse(res)
          this.loading = false
          if (this.csvErrors.length === 0) {
            this.eventService.broadcast(new Event(EventType.MEMBER_LIST_MODIFICATION, 'New member uploaded'))
            this.activeModal.dismiss(true)
          }
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
  selector: 'app-member-import-popup',
  template: '',
})
export class MemberImportPopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef | undefined | null

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal
  ) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ msMember }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(MemberImportDialogComponent as Component, {
          size: 'lg',
          backdrop: 'static',
        })
        this.ngbModalRef.componentInstance.msMember = msMember
        this.ngbModalRef.result.then(
          (result) => {
            this.router.navigate(['/members', { outlets: { popup: null } }])
            this.ngbModalRef = null
          },
          (reason) => {
            this.router.navigate(['/members', { outlets: { popup: null } }])
            this.ngbModalRef = null
          }
        )
      }, 0)
    })
  }

  ngOnDestroy() {
    this.ngbModalRef = null
  }
}
