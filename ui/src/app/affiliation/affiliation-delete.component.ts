import { Component, OnInit, OnDestroy } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'

import { AffiliationService } from './service/affiliation.service'
import { EventService } from 'src/app/shared/service/event.service'
import { AlertService } from 'src/app/shared/service/alert.service'
import { IAffiliation } from './model/affiliation.model'
import { AFFILIATION_STATUS } from 'src/app/shared/constants/orcid-api.constants'
import { AlertType, EventType } from 'src/app/app.constants'
import { Event } from 'src/app/shared/model/event.model'
import { faBan, faTimes } from '@fortawesome/free-solid-svg-icons'

@Component({
  selector: 'app-affiliation-delete-dialog',
  templateUrl: './affiliation-delete.component.html',
})
export class AffiliationDeleteDialogComponent implements OnInit {
  inOrcid: string = AFFILIATION_STATUS.IN_ORCID
  userRevokedAccess: string = AFFILIATION_STATUS.USER_REVOKED_ACCESS
  affiliation: IAffiliation | undefined
  errorUserRevoked = false
  faTimes = faTimes
  faBan = faBan
  message = ''

  constructor(
    protected affiliationService: AffiliationService,
    public activeModal: NgbActiveModal,
    protected eventService: EventService,
    private alertService: AlertService
  ) {}

  clear() {
    this.activeModal.dismiss(true)
  }

  ngOnInit(): void {
    this.message = $localize`:@@gatewayApp.assertionServiceAssertion.delete.fromPortalAndRegistry.string:Are you sure you want to delete this affiliation for ${this.affiliation?.email}? The affiliation will be deleted from the portal and
    the user's ORCID record`
  }

  confirmDelete(id: string | undefined) {
    console.log(id)

    if (id) {
      this.affiliationService.delete(id).subscribe((response) => {
        if (response.body.deleted) {
          this.eventService.broadcast(new Event(EventType.AFFILIATION_LIST_MODIFICATION, 'Deleted an affiliation'))
          this.alertService.broadcast(AlertType.AFFILIATION_DELETED)
        } else {
          this.eventService.broadcast(
            new Event(EventType.AFFILIATION_LIST_MODIFICATION, 'Failed to delete an affiliation')
          )
          this.alertService.broadcast(AlertType.AFFILIATION_DELETE_FAILURE)
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
  protected ngbModalRef: NgbModalRef | undefined

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal
  ) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ affiliation }) => {
      console.log(affiliation)

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
