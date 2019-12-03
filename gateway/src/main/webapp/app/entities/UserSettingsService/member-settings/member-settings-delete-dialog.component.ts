import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';
import { MemberSettingsService } from './member-settings.service';

@Component({
  selector: 'jhi-member-settings-delete-dialog',
  templateUrl: './member-settings-delete-dialog.component.html'
})
export class MemberSettingsDeleteDialogComponent {
  memberSettings: IMemberSettings;

  constructor(
    protected memberSettingsService: MemberSettingsService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager
  ) {}

  clear() {
    this.activeModal.dismiss('cancel');
  }

  confirmDelete(id: string) {
    this.memberSettingsService.delete(id).subscribe(response => {
      this.eventManager.broadcast({
        name: 'memberSettingsListModification',
        content: 'Deleted an memberSettings'
      });
      this.activeModal.dismiss(true);
    });
  }
}

@Component({
  selector: 'jhi-member-settings-delete-popup',
  template: ''
})
export class MemberSettingsDeletePopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ memberSettings }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(MemberSettingsDeleteDialogComponent as Component, { size: 'lg', backdrop: 'static' });
        this.ngbModalRef.componentInstance.memberSettings = memberSettings;
        this.ngbModalRef.result.then(
          result => {
            this.router.navigate(['/member-settings', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/member-settings', { outlets: { popup: null } }]);
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
