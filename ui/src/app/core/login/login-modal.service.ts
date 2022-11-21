import { Injectable } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

// import { JhiLoginModalComponent } from 'app/shared/login/login.component';

@Injectable({ providedIn: 'root' })
export class LoginModalService {
  private isOpen = false;
  constructor(private modalService: NgbModal) {}
  // TODO: rewrite completely?
  open(): NgbModalRef | null {
    if (this.isOpen) {
      return null;
    }
    this.isOpen = true;
    /*const modalRef = this.modalService.open(JhiLoginModalComponent);
    modalRef.result.then(
      result => {
        this.isOpen = false;
      },
      reason => {
        this.isOpen = false;
      }
    );
    return modalRef;*/
    return null;
  }
}
