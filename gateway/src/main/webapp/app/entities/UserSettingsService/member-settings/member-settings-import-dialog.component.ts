import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';
import { MemberSettingsService } from './member-settings.service';

import { FileUploadService } from '../../../shared/fileUpload/fileUpload.service';

import { SERVER_API_URL } from 'app/app.constants';

@Component({
  selector: 'jhi-member-settings-import-dialog',
  templateUrl: './member-settings-import-dialog.component.html',
  providers: [FileUploadService]
})
export class MemberSettingsImportDialogComponent {

  public resourceUrl;
  memberSettings: IMemberSettings;
  isSaving: boolean;
  currentFile: FileList;	
  csvErrors: any;

  constructor(
    protected memberSettingsService: MemberSettingsService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager,
    private uploadService: FileUploadService
  ) {
	  this.isSaving = false;
	  this.resourceUrl = this.memberSettingsService.resourceUrl + '/upload';
  }

  clear() {
    this.activeModal.dismiss('cancel');
  }

  selectFile(event) {
      this.currentFile = event.target.files;
  }
  
  upload() {    
  	var f = this.currentFile.item(0);
      this.uploadService.uploadFile(this.resourceUrl, f).subscribe(event => {
      	  if (event instanceof HttpResponse) {                               
         		var body = event.body;
         		this.csvErrors = JSON.parse(body.toString()); 
         		if(this.csvErrors.length == 0) {
         		  this.eventManager.broadcast({
                    name: 'memberSettingsListModification',
                    content: 'New member settings uploaded'
                  });
         	      this.activeModal.dismiss(true);
         		} 
          }
      });        
  }
}

@Component({
  selector: 'jhi-member-settings-import-popup',
  template: ''
})
export class MemberSettingsImportPopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ memberSettings }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(MemberSettingsImportDialogComponent as Component, { size: 'lg', backdrop: 'static' });
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
