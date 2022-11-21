import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpResponse } from '@angular/common/http';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMSMember } from 'app/shared/model/member.model';
import { MSMemberService } from './member.service';

import { FileUploadService } from 'app/shared/fileUpload/fileUpload.service';

import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'jhi-ms-member-import-dialog',
  templateUrl: './member-import-dialog.component.html',
  providers: [FileUploadService]
})
export class MSMemberImportDialogComponent {
  public resourceUrl;
  msMember: IMSMember;
  isSaving: boolean;
  currentFile: FileList;
  csvErrors: any;
  loading = false;

  constructor(
    protected msMemberService: MSMemberService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager,
    protected translate: TranslateService,
    private uploadService: FileUploadService
  ) {
    this.isSaving = false;
    this.resourceUrl = this.msMemberService.resourceUrl + '/upload';
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
              name: 'msMemberListModification',
              content: 'New member uploaded'
            });
            this.activeModal.dismiss(true);
          }
        }
      });
    } else {
      alert(this.translate.instant('gatewayApp.msUserServiceMSUser.import.emptyFile.string'));
    }
  }
}

@Component({
  selector: 'jhi-ms-member-import-popup',
  template: ''
})
export class MSMemberImportPopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ msMember }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(MSMemberImportDialogComponent as Component, { size: 'lg', backdrop: 'static' });
        this.ngbModalRef.componentInstance.msMember = msMember;
        this.ngbModalRef.result.then(
          result => {
            this.router.navigate(['/member', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/member', { outlets: { popup: null } }]);
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
