import { Component, OnDestroy, OnInit } from '@angular/core'
import { UserService } from './service/user.service'
import { AlertService } from '../shared/service/alert.service'
import { IUser } from './model/user.model'
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'
import { EventService } from '../shared/service/event.service'
import { ActivatedRoute, Router } from '@angular/router'
import { Event } from '../shared/model/event.model'
import { AlertType, EventType } from '../app.constants'

@Component({
  selector: 'app-user-delete-dialog',
  templateUrl: './user-delete.component.html',
})
export class UserDeleteDialogComponent implements OnInit {
  user: IUser | undefined
  message = ''

  constructor(
    protected userService: UserService,
    public activeModal: NgbActiveModal,
    protected eventService: EventService,
    private alertService: AlertService
  ) {}

  clear() {
    this.activeModal.dismiss('cancel')
  }

  confirmDelete(id: string | undefined) {
    if (id) {
      this.userService.delete(id).subscribe(() => {
        this.eventService.broadcast({
          type: EventType.USER_LIST_MODIFIED,
          payload: 'Deleted a user',
        } as Event)
        this.activeModal.dismiss(true)
        this.alertService.broadcast(AlertType.USER_DELETED)
      })
    }
  }

  ngOnInit(): void {
    this.message = $localize`:@@gatewayApp.msUserServiceMSUser.delete.question.string:Are you sure you want to delete user ${this.user?.email}?`
  }
}

@Component({
  selector: 'app-user-delete-popup',
  template: '',
})
export class UserDeletePopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef | undefined

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal
  ) {}
  ngOnInit() {
    this.activatedRoute.data.subscribe(({ user }) => {
      setTimeout(() => {
        console.log(user)

        this.ngbModalRef = this.modalService.open(UserDeleteDialogComponent as Component, {
          size: 'lg',
          backdrop: 'static',
        })
        this.ngbModalRef.componentInstance.user = user
        this.ngbModalRef.result.then(
          (result) => {
            this.router.navigate(['/users', { outlets: { popup: null } }])
            this.ngbModalRef = undefined
          },
          (reason) => {
            this.router.navigate(['/users', { outlets: { popup: null } }])
            this.ngbModalRef = undefined
          }
        )
      }, 100)
    })
  }

  ngOnDestroy() {
    this.ngbModalRef = undefined
  }
}
