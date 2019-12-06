import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IUserSettings } from 'app/shared/model/UserSettingsService/user-settings.model';
import { UserSettingsService } from './user-settings.service';

import { FileUploadService } from '../../../shared/fileUpload/fileUpload.service';

import { SERVER_API_URL } from 'app/app.constants';

@Component({
  selector: 'jhi-user-settings-import-dialog',
  templateUrl: './user-settings-import-dialog.component.html',
  providers: [FileUploadService]
})
export class UserSettingsImportDialogComponent {

  public resourceUrl;
  userSettings: IUserSettings;
  isSaving: boolean;
  currentFile: FileList;	
  csvErrors: any;

  constructor(
    protected userSettingsService: UserSettingsService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager,
    private uploadService: FileUploadService
  ) {
	  this.isSaving = false;
	  this.resourceUrl = this.userSettingsService.resourceUrl + '/upload';
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
                    name: 'userSettingsListModification',
                    content: 'New user settings uploaded'
                  });
         	      this.activeModal.dismiss(true);
         		} 
          }
      });        
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
