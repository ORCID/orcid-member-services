import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IAssertion } from 'app/shared/model/assertion.model';
import { AssertionService } from './assertion.service';
import { ASSERTION_STATUS } from 'app/shared/constants/orcid-api.constants';

import { JhiAlertService } from 'ng-jhipster';

@Component({
  selector: 'jhi-assertion-delete-dialog',
  templateUrl: './assertion-delete-dialog.component.html'
})
export class AssertionDeleteDialogComponent {
  inOrcid: string = ASSERTION_STATUS.IN_ORCID;
  userRevokedAccess: string = ASSERTION_STATUS.USER_REVOKED_ACCESS;
  assertion: IAssertion;
  errorUserRevoked = false;

  constructor(
    protected assertionService: AssertionService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager,
    private alertService: JhiAlertService
  ) {}
  clear() {
    this.activeModal.dismiss(true);
  }

  confirmDelete() {
    this.assertionService.delete(this.assertion.id).subscribe(response => {
      if (response.body.deleted) {
        this.eventManager.broadcast({
          name: 'assertionListModification',
          content: 'Deleted an assertion'
        });
        this.alertService.success('assertionServiceApp.affiliation.deleted.string');
      } else {
        this.eventManager.broadcast({
          name: 'assertionListModification',
          content: 'Failed to delete an assertion'
        });
        this.alertService.warning('assertionServiceApp.affiliation.problemDeleting.string');
      }
      this.activeModal.dismiss(true);
    });
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
