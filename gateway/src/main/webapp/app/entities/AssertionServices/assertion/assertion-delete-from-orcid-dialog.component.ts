import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IAssertion } from 'app/shared/model/AssertionServices/assertion.model';
import { AssertionService } from './assertion.service';

@Component({
  selector: 'jhi-assertion-delete-from-orcid-dialog',
  templateUrl: './assertion-delete-from-orcid-dialog.component.html'
})
export class AssertionDeleteFromOrcidDialogComponent {
  assertion: IAssertion;

  constructor(
    protected assertionService: AssertionService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager
  ) {}

  clear() {
    this.activeModal.dismiss('cancel');
  }

  confirmDelete(id: string) {
    this.assertionService.deleteFromOrcid(id).subscribe(response => {
      this.eventManager.broadcast({
        name: 'assertionListModification',
        content: 'Deleted an assertion from ORCID'
      });
      this.activeModal.dismiss(true);
    });
  }
}

@Component({
  selector: 'jhi-assertion-delete-from-orcid-popup',
  template: ''
})
export class AssertionDeleteFromOrcidPopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ assertion }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(AssertionDeleteFromOrcidDialogComponent as Component, { size: 'lg', backdrop: 'static' });
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
