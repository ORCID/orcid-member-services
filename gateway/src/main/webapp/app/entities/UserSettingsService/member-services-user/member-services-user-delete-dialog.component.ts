import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMemberServicesUser } from 'app/shared/model/UserSettingsService/member-services-user.model';
import { MemberServicesUserService } from './member-services-user.service';

@Component({
  selector: 'jhi-member-services-user-delete-dialog',
  templateUrl: './member-services-user-delete-dialog.component.html'
})
export class MemberServicesUserDeleteDialogComponent {
  memberServicesUser: IMemberServicesUser;

  constructor(
    protected memberServicesUserService: MemberServicesUserService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager
  ) {}

  clear() {
    this.activeModal.dismiss('cancel');
  }

  confirmDelete(id: string) {
    this.memberServicesUserService.delete(id).subscribe(response => {
      this.eventManager.broadcast({
        name: 'memberServicesUserListModification',
        content: 'Deleted an memberServicesUser'
      });
      this.activeModal.dismiss(true);
    });
  }
}

@Component({
  selector: 'jhi-member-services-user-delete-popup',
  template: ''
})
export class MemberServicesUserDeletePopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ memberServicesUser }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(MemberServicesUserDeleteDialogComponent as Component, { size: 'lg', backdrop: 'static' });
        this.ngbModalRef.componentInstance.memberServicesUser = memberServicesUser;
        this.ngbModalRef.result.then(
          result => {
            this.router.navigate(['/member-services-user', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/member-services-user', { outlets: { popup: null } }]);
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
