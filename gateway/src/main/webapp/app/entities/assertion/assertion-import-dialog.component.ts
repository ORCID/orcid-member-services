import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IAssertion } from 'app/shared/model/assertion.model';
import { AssertionService } from './assertion.service';

import { FileUploadService } from 'app/shared/fileUpload/fileUpload.service';

import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'jhi-assertions-import-dialog',
  templateUrl: './assertion-import-dialog.component.html',
  providers: [FileUploadService]
})
export class AssertionImportDialogComponent {
  public resourceUrl;
  assertion: IAssertion;
  isSaving: boolean;
  currentFile: FileList;
  success: Boolean;
  uploaded: boolean;
  loading = false;

  constructor(
    protected assertionService: AssertionService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager,
    private uploadService: FileUploadService,
    protected translate: TranslateService
  ) {
    this.isSaving = false;
    this.resourceUrl = this.assertionService.resourceUrl + '/upload';
    this.success = false;
    this.uploaded = false;
  }

  clear() {
    this.activeModal.dismiss(true);
    window.history.back();
  }

  selectFile(event) {
    this.currentFile = event.target.files;
  }

  upload() {
    if (this.currentFile) {
      this.loading = true;
      const f = this.currentFile.item(0);
      this.uploadService.uploadFile(this.resourceUrl, f, 'json').subscribe(event => {
        if (event instanceof HttpResponse) {
          this.success = event.body;
          this.uploaded = true;
          this.loading = false;
        }
      });
    } else {
      alert(this.translate.instant('gatewayApp.msUserServiceMSUser.import.emptyFile.string'));
    }
  }

  close() {
    this.eventManager.broadcast({
      name: 'importAssertions',
      content: ''
    });
    this.activeModal.dismiss(true);
  }
}

@Component({
  selector: 'jhi-assertions-import-popup',
  template: ''
})
export class AssertionImportPopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ assertion }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(AssertionImportDialogComponent as Component, { size: 'lg', backdrop: 'static' });
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
