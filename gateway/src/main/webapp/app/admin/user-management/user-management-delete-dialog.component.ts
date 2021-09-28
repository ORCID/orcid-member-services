import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { User, UserService } from 'app/core';

@Component({
  selector: 'jhi-user-mgmt-delete-dialog',
  templateUrl: './user-management-delete-dialog.component.html'
})
export class UserMgmtDeleteDialogComponent {
  user: User;

  constructor(private userService: UserService, public activeModal: NgbActiveModal, private eventManager: JhiEventManager) {}

  clear() {
    this.activeModal.dismiss('cancel');
  }

  confirmDelete(email) {
    this.userService.delete(email).subscribe(response => {
      this.eventManager.broadcast({ name: 'userListModification', content: 'Deleted a user' });
      this.activeModal.close(true);
    });
  }
}
