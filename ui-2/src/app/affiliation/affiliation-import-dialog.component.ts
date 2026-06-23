import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { faBan, faFolderOpen, faPlus } from '@fortawesome/free-solid-svg-icons'
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'
import { EventType } from '../app.constants'
import { Event } from '../shared/model/event.model'
import { EventService } from '../shared/service/event.service'
import { FileUploadService } from '../shared/service/file-upload.service'
import { IAffiliation } from './model/affiliation.model'
import { AffiliationService } from './service/affiliation.service'
import { ReactiveFormsModule, FormsModule } from '@angular/forms'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { NgbAlertModule } from '@ng-bootstrap/ng-bootstrap'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'

@Component({
  selector: 'app-affiliation-import-dialog',
  templateUrl: './affiliation-import-dialog.component.html',
  styleUrls: ['./affiliation-import-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FormsModule, ErrorAlertComponent, NgbAlertModule, FaIconComponent],
})
export class AffiliationImportDialogComponent {
  protected affiliationService = inject(AffiliationService)
  protected activeModal = inject(NgbActiveModal)
  protected eventService = inject(EventService)
  private uploadService = inject(FileUploadService)

  protected resourceUrl: string
  protected affiliation: IAffiliation | undefined
  protected isSaving = signal(false)
  protected currentFile = signal<FileList | undefined>(undefined)
  protected success = signal(false)
  protected uploaded = signal(false)
  protected loading = signal(false)
  protected selectedFileName = signal('No file selected.')
  protected faBan = faBan
  protected faPlus = faPlus
  protected faFolderOpen = faFolderOpen

  constructor() {
    this.resourceUrl = this.affiliationService.resourceUrl + '/upload'
  }

  clear() {
    this.activeModal.dismiss('cancel')
  }

  selectFile(event: any) {
    this.currentFile.set(event.target.files)
    this.selectedFileName.set(this.currentFile()?.item(0)?.name ?? 'No file selected.')
  }

  clearFileInput(event: MouseEvent) {
    (event.target as HTMLInputElement).value = ''
    this.selectedFileName.set('No file selected.')
    this.currentFile.set(undefined)
  }

  upload() {
    if (this.currentFile()) {
      this.loading.set(true)
      const f = this.currentFile()!.item(0)
      this.uploadService.uploadFile(this.resourceUrl, f!, 'json').subscribe((event: string) => {
        this.success.set(true)
        this.uploaded.set(true)
        this.loading.set(false)
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
