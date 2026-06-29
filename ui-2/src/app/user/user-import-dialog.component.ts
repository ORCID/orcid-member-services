import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal } from '@angular/core'
import { FileUploadService } from '../shared/service/file-upload.service'
import { UserService } from './service/user.service'
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'
import { EventService } from '../shared/service/event.service'
import { Event as AppEvent } from '../shared/model/event.model'
import { EventType } from '../app.constants'
import { ActivatedRoute, Router } from '@angular/router'
import { faSave, faBan } from '@fortawesome/free-solid-svg-icons'
import { ReactiveFormsModule, FormsModule } from '@angular/forms'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { NgbAlertModule } from '@ng-bootstrap/ng-bootstrap'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'

@Component({
  selector: 'app-user-import-dialog',
  templateUrl: './user-import-dialog.component.html',
  styleUrls: ['./user-import-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FormsModule, ErrorAlertComponent, NgbAlertModule, FaIconComponent],
})
export class UserImportDialogComponent {
  protected userService = inject(UserService)
  protected activeModal = inject(NgbActiveModal)
  protected eventService = inject(EventService)
  private fileUploadService = inject(FileUploadService)

  protected resourceUrl = this.userService.resourceUrl + '/upload'
  protected isSaving = signal(false)
  protected currentFile = signal<FileList | null>(null)
  protected csvErrors = signal<{ index: number; message: string }[] | null>(null)
  protected loading = signal(false)
  protected faBan = faBan
  protected faSave = faSave

  clear() {
    this.activeModal.dismiss('cancel')
  }

  selectFile(event: Event) {
    this.currentFile.set((event.target as HTMLInputElement).files)
  }

  clearFileInput(event: MouseEvent) {
    (event.target as HTMLInputElement).value = ''
    this.currentFile.set(null)
  }

  upload() {
    const currentFile = this.currentFile()
    if (currentFile) {
      this.loading.set(true)
      const f = currentFile.item(0)

      if (!f) {
        this.loading.set(false)
        return
      }

      this.fileUploadService.uploadFile(this.resourceUrl, f, 'text').subscribe((res: string) => {
        if (res) {
          const errors = JSON.parse(res) as { index: number; message: string }[]
          this.csvErrors.set(errors)
          this.loading.set(false)
          if (errors.length === 0) {
            this.eventService.broadcast(new AppEvent(EventType.USER_LIST_MODIFIED))
            this.activeModal.dismiss(true)
          }
        }
      })
    } else {
      alert(
        $localize`:@@gatewayApp.msUserServiceMSUser.import.emptyFile.string:There is no file to upload. Please select one.`
      )
    }
  }
}

@Component({
  selector: 'app-user-import-popup',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserImportPopupComponent implements OnInit, OnDestroy {
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected modalService = inject(NgbModal)

  protected ngbModalRef: NgbModalRef | undefined | null

  ngOnInit() {
    setTimeout(() => {
      this.ngbModalRef = this.modalService.open(UserImportDialogComponent as Component, {
        size: 'lg',
        backdrop: 'static',
      })
      this.ngbModalRef.result.then(
        () => {
          this.router.navigate(['/users', { outlets: { popup: null } }])
          this.ngbModalRef = null
        },
        () => {
          this.router.navigate(['/users', { outlets: { popup: null } }])
          this.ngbModalRef = null
        }
      )
    }, 0)
  }

  ngOnDestroy() {
    this.ngbModalRef = null
    this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => this.router.navigate(['/users']))
  }
}
