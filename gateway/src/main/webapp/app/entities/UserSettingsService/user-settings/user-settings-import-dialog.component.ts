import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IUserSettings } from 'app/shared/model/UserSettingsService/user-settings.model';
import { UserSettingsService } from './user-settings.service';

@Component({
  selector: 'jhi-user-settings-import-dialog',
  templateUrl: './user-settings-import-dialog.component.html'
})
export class UserSettingsImportDialogComponent {
  userSettings: IUserSettings;

  constructor(
    protected userSettingsService: UserSettingsService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager
  ) {}

  clear() {
    this.activeModal.dismiss('cancel');
  }

  uploadUsers() {
    
  }
}

@Component({
  selector: 'jhi-user-settings-import-popup',
  template: ''
})
export class UserSettingsImportPopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ userSettings }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(UserSettingsImportDialogComponent as Component, { size: 'lg', backdrop: 'static' });
        this.ngbModalRef.componentInstance.userSettings = userSettings;
        this.ngbModalRef.result.then(
          result => {
            this.router.navigate(['/user-settings', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/user-settings', { outlets: { popup: null } }]);
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
