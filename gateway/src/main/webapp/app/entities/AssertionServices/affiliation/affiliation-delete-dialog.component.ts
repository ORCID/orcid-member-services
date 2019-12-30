import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IAffiliation } from 'app/shared/model/AssertionServices/affiliation.model';
import { AffiliationService } from './affiliation.service';

@Component({
  selector: 'jhi-affiliation-delete-dialog',
  templateUrl: './affiliation-delete-dialog.component.html'
})
export class AffiliationDeleteDialogComponent {
  affiliation: IAffiliation;

  constructor(
    protected affiliationService: AffiliationService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager
  ) {}

  clear() {
    this.activeModal.dismiss('cancel');
  }

  confirmDelete(id: string) {
    this.affiliationService.delete(id).subscribe(response => {
      this.eventManager.broadcast({
        name: 'affiliationListModification',
        content: 'Deleted an affiliation'
      });
      this.activeModal.dismiss(true);
    });
  }
}

@Component({
  selector: 'jhi-affiliation-delete-popup',
  template: ''
})
export class AffiliationDeletePopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ affiliation }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(AffiliationDeleteDialogComponent as Component, { size: 'lg', backdrop: 'static' });
        this.ngbModalRef.componentInstance.affiliation = affiliation;
        this.ngbModalRef.result.then(
          result => {
            this.router.navigate(['/affiliation', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/affiliation', { outlets: { popup: null } }]);
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
