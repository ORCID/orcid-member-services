import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMSUser } from 'app/shared/model/user.model';
import { MSUserService } from './user.service';

import { JhiAlertService } from 'ng-jhipster';

@Component({
  selector: 'jhi-ms-user-delete-dialog',
  templateUrl: './user-delete-dialog.component.html'
})
export class MSUserDeleteDialogComponent {
  msUser: IMSUser;

  constructor(
    protected msUserService: MSUserService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager,
    private alertService: JhiAlertService
  ) {}

  clear() {
    this.activeModal.dismiss('cancel');
  }

  confirmDelete(id: string) {
    this.msUserService.delete(id).subscribe(response => {
      this.eventManager.broadcast({
        name: 'msUserListModification',
        content: 'Deleted an msUser'
      });
      this.activeModal.dismiss(true);
      this.alertService.success('userServiceApp.user.deleted.string');
    });
  }
}

@Component({
  selector: 'jhi-ms-user-delete-popup',
  template: ''
})
export class MSUserDeletePopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ msUser }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(MSUserDeleteDialogComponent as Component, { size: 'lg', backdrop: 'static' });
        this.ngbModalRef.componentInstance.msUser = msUser;
        this.ngbModalRef.result.then(
          result => {
            this.router.navigate(['/user', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/user', { outlets: { popup: null } }]);
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
