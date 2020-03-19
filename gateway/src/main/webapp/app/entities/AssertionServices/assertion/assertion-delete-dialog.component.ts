import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IAssertion } from 'app/shared/model/AssertionServices/assertion.model';
import { AssertionService } from './assertion.service';

@Component({
  selector: 'jhi-assertion-delete-dialog',
  templateUrl: './assertion-delete-dialog.component.html'
})
export class AssertionDeleteDialogComponent {
  assertion: IAssertion;
  errorDeletingFromOrcid: boolean;

  constructor(
    protected assertionService: AssertionService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager
  ) {
    this.errorDeletingFromOrcid = false;
  }

  clear() {
    this.activeModal.dismiss('cancel');
  }

  confirmDelete(id: string, putCode?: string) {
    this.errorDeletingFromOrcid = false;
    if (putCode) {
      this.assertionService.deleteFromOrcid(id).subscribe(res => {
        // TODO: add response code to res.body
        // if 404, delete from assertion service
        // if 403, change assertion status to token revoked
        if (res.body.deleted === 'true') {
          this.assertionService.delete(id).subscribe(response => {
            this.activeModal.dismiss(true);
            this.eventManager.broadcast({
              name: 'assertionListModification',
              content: 'Deleted an assertion'
            });
            this.activeModal.dismiss(true);
          });
        } else {
          this.errorDeletingFromOrcid = true;
        }
      });
    } else {
      this.assertionService.delete(id).subscribe(response => {
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
            this.router.navigate(['/assertions', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/assertions', { outlets: { popup: null } }]);
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
