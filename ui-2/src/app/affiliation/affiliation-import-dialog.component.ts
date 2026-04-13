import { Component, OnDestroy, OnInit, inject } from '@angular/core'
import { IAffiliation } from './model/affiliation.model'
import { AffiliationService } from './service/affiliation.service'
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'
import { EventService } from '../shared/service/event.service'
import { FileUploadService } from '../shared/service/file-upload.service'
import { HttpResponse } from '@angular/common/http'
import { EventType } from '../app.constants'
import { Event } from '../shared/model/event.model'
import { ActivatedRoute, Router } from '@angular/router'
import { faBan, faPlus } from '@fortawesome/free-solid-svg-icons'

@Component({
  selector: 'app-affiliation-import-dialog',
  templateUrl: './affiliation-import-dialog.component.html',
  styleUrls: ['./affiliation-import-dialog.component.scss'],
  standalone: false,
})
export class AffiliationImportDialogComponent {
  protected affiliationService = inject(AffiliationService)
  activeModal = inject(NgbActiveModal)
  protected eventService = inject(EventService)
  private uploadService = inject(FileUploadService)

  public resourceUrl
  affiliation: IAffiliation | undefined
  isSaving: boolean
  currentFile: FileList | undefined
  success: boolean
  uploaded: boolean
  loading = false
  faBan = faBan
  faPlus = faPlus

  constructor() {
    this.isSaving = false
    this.resourceUrl = this.affiliationService.resourceUrl + '/upload'
    this.success = false
    this.uploaded = false
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
      this.uploadService.uploadFile(this.resourceUrl, f!, 'json').subscribe((event: string) => {
        this.success = true
        this.uploaded = true
        this.loading = false
      })
    } else {
      alert(
        $localize`:@@gatewayApp.msUserServiceMSUser.import.emptyFile.string:There is no file to upload. Please select one.`
      )
    }
  }

  close() {
    this.eventService.broadcast(new Event(EventType.IMPORT_AFFILIATIONS))
    this.activeModal.dismiss(true)
  }
}

@Component({
  selector: 'app-affiliations-import-popup',
  template: '',
  standalone: false,
})
export class AffiliationImportPopupComponent implements OnInit, OnDestroy {
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected modalService = inject(NgbModal)

  protected ngbModalRef: NgbModalRef | undefined | null

  ngOnInit() {
    setTimeout(() => {
      this.ngbModalRef = this.modalService.open(AffiliationImportDialogComponent as Component, {
        size: 'lg',
        backdrop: 'static',
      })

      this.ngbModalRef.result.then(
        (result) => {
          this.router.navigate(['/affiliations', { outlets: { popup: null } }])
          this.ngbModalRef = null
        },
        (reason) => {
          this.router.navigate(['/affiliations', { outlets: { popup: null } }])
          this.ngbModalRef = null
        }
      )
    }, 0)
  }

  ngOnDestroy() {
    this.ngbModalRef = null
    this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => this.router.navigate(['/affiliations']))
  }
}
