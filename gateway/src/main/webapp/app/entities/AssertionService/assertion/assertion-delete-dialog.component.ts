import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IAssertion } from 'app/shared/model/AssertionService/assertion.model';
import { AssertionService } from './assertion.service';
import { ASSERTION_STATUS } from 'app/shared/constants/orcid-api.constants';

@Component({
  selector: 'jhi-assertion-delete-dialog',
  templateUrl: './assertion-delete-dialog.component.html'
})
export class AssertionDeleteDialogComponent {
  inOrcid: string = ASSERTION_STATUS.IN_ORCID;
  userRevokedAccess: string = ASSERTION_STATUS.USER_REVOKED_ACCESS;
  assertion: IAssertion;
  errorDeletingFromOrcid: boolean;
  errorUserRevoked = false;

  constructor(protected assertionService: AssertionService, public activeModal: NgbActiveModal, protected eventManager: JhiEventManager) {
    this.errorDeletingFromOrcid = false;
  }

  clear() {
    this.activeModal.dismiss(true);
  }

  confirmDelete() {
    this.errorDeletingFromOrcid = false;
    if (
      this.assertion.putCode &&
      (this.assertion.status === ASSERTION_STATUS.IN_ORCID || this.assertion.status === ASSERTION_STATUS.ERROR_UPDATING_IN_ORCID)
    ) {
      this.assertionService.deleteFromOrcid(this.assertion.id).subscribe(res => {
        if (res.body.deleted === true || res.body.statusCode === 404) {
          this.assertionService.delete(this.assertion.id).subscribe(response => {
            this.activeModal.dismiss(true);
            this.eventManager.broadcast({
              name: 'assertionListModification',
              content: 'Deleted an assertion'
            });
            this.activeModal.dismiss(true);
          });
        } else {
          this.errorDeletingFromOrcid = true;
          // TODO: API returns incorrect status code
          // Change to 401 when this problem is corrected
          if (res.body.statusCode === 400 || res.body.statusCode === 401) {
            this.errorUserRevoked = true;
          }
        }
      });
    } else {
      this.assertionService.delete(this.assertion.id).subscribe(response => {
        this.activeModal.dismiss(true);
        this.eventManager.broadcast({
          name: 'assertionListModification',
          content: 'Deleted an assertion'
        });
        this.activeModal.dismiss(true);
      });
    }
  }
}

@Component({
  selector: 'jhi-assertion-delete-popup',
  template: ''
})
export class AssertionDeletePopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ assertion }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(AssertionDeleteDialogComponent as Component, { size: 'lg', backdrop: 'static' });
        this.ngbModalRef.componentInstance.assertion = assertion;
        this.ngbModalRef.result.then(
          result => {
            this.router.navigate(['/assertion', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/assertion', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          }
        );
      }, 0);
    });
  }

  ngOnDestroy() {
    this.ngbModalRef = null;
  }
}
