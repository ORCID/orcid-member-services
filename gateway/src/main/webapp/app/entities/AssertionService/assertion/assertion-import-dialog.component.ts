import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IAssertion } from 'app/shared/model/AssertionService/assertion.model';
import { AssertionService } from './assertion.service';

import { FileUploadService } from '../../../shared/fileUpload/fileUpload.service';

import { SERVER_API_URL } from 'app/app.constants';

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
  csvErrors: any;

  constructor(
    protected assertionService: AssertionService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager,
    private uploadService: FileUploadService
  ) {
    this.isSaving = false;
    this.resourceUrl = this.assertionService.resourceUrl + '/upload';
  }

  clear() {
    this.activeModal.dismiss('cancel');
  }

  selectFile(event) {
    this.currentFile = event.target.files;
  }

  upload() {
    const f = this.currentFile.item(0);
    this.uploadService.uploadFile(this.resourceUrl, f).subscribe(event => {
      if (event instanceof HttpResponse) {
        const body = event.body;
        this.csvErrors = JSON.parse(body.toString());
        if (this.csvErrors.length === 0) {
          this.eventManager.broadcast({
            name: 'assertionListModification',
            content: 'New assertions uploaded'
          });
          this.activeModal.dismiss(true);
        }
      }
    });
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
            this.router.navigate(['/assertions', { outlets: { popup: null } }]);
            this.ngbModalRef = null;
          },
          reason => {
            this.router.navigate(['/assertions', { outlets: { popup: null } }]);
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
