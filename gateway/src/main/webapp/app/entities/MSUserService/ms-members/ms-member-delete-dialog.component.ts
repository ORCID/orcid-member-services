import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMSMember } from 'app/shared/model/MSUserService/ms-member.model';
import { MSMemberService } from './ms-member.service';

import { JhiAlertService } from 'ng-jhipster';

@Component({
  selector: 'jhi-ms-member-delete-dialog',
  templateUrl: './ms-member-delete-dialog.component.html'
})
export class MSMemberDeleteDialogComponent {
  msMember: IMSMember;
  loading = false;

  constructor(
    protected msMemberService: MSMemberService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager,
    private alertService: JhiAlertService
  ) {}

  clear() {
    this.activeModal.dismiss('cancel');
  }

  confirmDelete(id: string) {
    this.loading = true;
    this.msMemberService.delete(id).subscribe(
      response => {
        this.loading = false;
        this.eventManager.broadcast({
          name: 'msMemberListModification',
          content: 'Deleted an msMember'
        });
        this.activeModal.dismiss(true);
        this.alertService.success('memberServiceApp.member.deleted.string');
      },
      error => {
        this.loading = false;
      }
    );
  }
}

@Component({
  selector: 'jhi-ms-member-delete-popup',
  template: ''
})
export class MSMemberDeletePopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ msMember }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(MSMemberDeleteDialogComponent as Component, { size: 'lg', backdrop: 'static' });
        this.ngbModalRef.componentInstance.msMember = msMember;
        this.ngbModalRef.result.then(
          result => {
            this.router.navigate(['/ms-member', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/ms-member', { outlets: { popup: null } }]);
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
