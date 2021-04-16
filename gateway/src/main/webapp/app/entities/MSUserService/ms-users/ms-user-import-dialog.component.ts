import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpResponse } from '@angular/common/http';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMSUser } from 'app/shared/model/MSUserService/ms-user.model';
import { MSUserService } from './ms-user.service';

import { FileUploadService } from 'app/shared/fileUpload/fileUpload.service';

import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'jhi-ms-user-import-dialog',
  templateUrl: './ms-user-import-dialog.component.html',
  providers: [FileUploadService]
})
export class MSUserImportDialogComponent {
  public resourceUrl;
  msUser: IMSUser;
  isSaving: boolean;
  currentFile: FileList;
  csvErrors: any;
  loading = false;

  constructor(
    protected msUserService: MSUserService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager,
    protected translate: TranslateService,
    private uploadService: FileUploadService
  ) {
    this.isSaving = false;
    this.resourceUrl = this.msUserService.resourceUrl + '/upload';
  }

  clear() {
    this.activeModal.dismiss('cancel');
  }

  selectFile(event) {
    this.currentFile = event.target.files;
  }

  upload() {
    if (this.currentFile) {
      this.loading = true;
      const f = this.currentFile.item(0);
      this.uploadService.uploadFile(this.resourceUrl, f, 'text').subscribe(event => {
        if (event instanceof HttpResponse) {
          const body = event.body;
          this.csvErrors = JSON.parse(body.toString());
          this.loading = false;
          if (this.csvErrors.length === 0) {
            this.eventManager.broadcast({
              name: 'msUserListModification',
              content: 'New user settings uploaded'
            });
            this.activeModal.dismiss(true);
          }
        }
      });
    } else {
      alert(this.translate.instant('gatewayApp.msUserServiceMSUser.import.emptyFile'));
    }
  }
}

@Component({
  selector: 'jhi-ms-user-import-popup',
  template: ''
})
export class MSUserImportPopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ msUser }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(MSUserImportDialogComponent as Component, { size: 'lg', backdrop: 'static' });
        this.ngbModalRef.componentInstance.msUser = msUser;
        this.ngbModalRef.result.then(
          result => {
            this.router.navigate(['/ms-user', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/ms-user', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          }
        );
      }, 0);
    });
  }

  ngOnDestroy() {
    this.ngbModalRef = null;
    this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => this.router.navigate(['/ms-user']));
  }
}
