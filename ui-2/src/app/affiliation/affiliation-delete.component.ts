import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, OnDestroy, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router } from '@angular/router'

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'

import { AffiliationService } from './service/affiliation.service'
import { EventService } from 'src/app/shared/service/event.service'
import { AlertService } from 'src/app/shared/service/alert.service'
import { IAffiliation } from './model/affiliation.model'
import { AFFILIATION_STATUS } from 'src/app/shared/constants/orcid-api.constants'
import { AlertMessage, AlertType, EventType } from 'src/app/app.constants'
import { Event } from 'src/app/shared/model/event.model'
import { faBan, faTimes } from '@fortawesome/free-solid-svg-icons'
import { ReactiveFormsModule, FormsModule } from '@angular/forms'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'

@Component({
  selector: 'app-affiliation-delete-dialog',
  templateUrl: './affiliation-delete.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FormsModule, ErrorAlertComponent, FaIconComponent],
})
export class AffiliationDeleteDialogComponent implements OnInit {
  protected affiliationService = inject(AffiliationService)
  protected activeModal = inject(NgbActiveModal)
  protected eventService = inject(EventService)
  private alertService = inject(AlertService)

  protected inOrcid: string = AFFILIATION_STATUS.IN_ORCID
  protected userRevokedAccess: string = AFFILIATION_STATUS.USER_REVOKED_ACCESS
  protected affiliation: IAffiliation | undefined
  protected errorUserRevoked = signal(false)
  protected faTimes = faTimes
  protected faBan = faBan
  protected message = signal('')

  clear() {
    this.activeModal.dismiss(true)
  }

  ngOnInit(): void {
    this.message.set(
      $localize`:@@gatewayApp.assertionServiceAssertion.delete.fromPortalAndRegistry.string:Are you sure you want to delete this affiliation for ${this.affiliation?.email}? The affiliation will be deleted from the portal and
    the user's ORCID record.`
    )
  }

  confirmDelete(id: string | undefined) {
    if (id) {
      this.affiliationService.delete(id).subscribe((response) => {
        if (response) {
          this.eventService.broadcast(new Event(EventType.AFFILIATION_LIST_MODIFICATION))
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.AFFILIATION_DELETED)
        } else {
          this.eventService.broadcast(new Event(EventType.AFFILIATION_LIST_MODIFICATION))
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.AFFILIATION_DELETE_FAILURE)
        }
        this.activeModal.dismiss(true)
      })
    }
  }
}

@Component({
  selector: 'app-affiliation-delete-popup',
  template: '',
})
export class AffiliationDeletePopupComponent implements OnInit, OnDestroy {
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected modalService = inject(NgbModal)
  private destroyRef = inject(DestroyRef)

  protected ngbModalRef: NgbModalRef | undefined

  ngOnInit() {
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ affiliation }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(AffiliationDeleteDialogComponent as Component, {
          size: 'lg',
          backdrop: 'static',
        })
        this.ngbModalRef.componentInstance.affiliation = affiliation
        this.ngbModalRef.result.then(
          (result) => {
            this.router.navigate(['/affiliations', { outlets: { popup: null } }])
            this.ngbModalRef = undefined
          },
          (reason) => {
            this.router.navigate(['/affiliations', { outlets: { popup: null } }])
            this.ngbModalRef = undefined
          }
        )
      }, 0)
    })
  }

  ngOnDestroy() {
    this.ngbModalRef = undefined
  }
}
