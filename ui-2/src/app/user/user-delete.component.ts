import { ChangeDetectionStrategy, Component, DestroyRef, OnDestroy, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { UserService } from './service/user.service'
import { AlertService } from '../shared/service/alert.service'
import { IUser } from './model/user.model'
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'
import { EventService } from '../shared/service/event.service'
import { ActivatedRoute, Router } from '@angular/router'
import { Event } from '../shared/model/event.model'
import { AlertMessage, AlertType, EventType } from '../app.constants'
import { faBan, faTimes } from '@fortawesome/free-solid-svg-icons'
import { ReactiveFormsModule, FormsModule } from '@angular/forms'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'

@Component({
  selector: 'app-user-delete-dialog',
  templateUrl: './user-delete.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FormsModule, ErrorAlertComponent, FaIconComponent],
})
export class UserDeleteDialogComponent implements OnInit {
  protected userService = inject(UserService)
  protected activeModal = inject(NgbActiveModal)
  protected eventService = inject(EventService)
  private alertService = inject(AlertService)

  protected user: IUser | undefined
  protected message = signal('')
  protected faBan = faBan
  protected faTimes = faTimes

  clear() {
    this.activeModal.dismiss('cancel')
  }

  confirmDelete(id: string | undefined) {
    if (id) {
      this.userService.delete(id).subscribe(() => {
        this.eventService.broadcast(new Event(EventType.USER_LIST_MODIFIED))
        this.activeModal.dismiss(true)
        this.alertService.broadcast(AlertType.TOAST, AlertMessage.USER_DELETED)
      })
    }
  }

  ngOnInit(): void {
    this.message.set(
      $localize`:@@gatewayApp.msUserServiceMSUser.delete.question.string:Are you sure you want to delete user ${this.user?.email}?`
    )
  }
}

@Component({
  selector: 'app-user-delete-popup',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserDeletePopupComponent implements OnInit, OnDestroy {
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected modalService = inject(NgbModal)
  private destroyRef = inject(DestroyRef)

  protected ngbModalRef: NgbModalRef | undefined
  ngOnInit() {
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ user }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(UserDeleteDialogComponent as Component, {
          size: 'lg',
          backdrop: 'static',
        })
        this.ngbModalRef.componentInstance.user = user
        this.ngbModalRef.result.then(
          () => {
            this.router.navigate(['/users', { outlets: { popup: null } }])
            this.ngbModalRef = undefined
          },
          () => {
            this.router.navigate(['/users', { outlets: { popup: null } }])
            this.ngbModalRef = undefined
          }
        )
      }, 0)
    })
  }

  ngOnDestroy() {
    this.ngbModalRef = undefined
  }
}
