import { ChangeDetectionStrategy, Component, DestroyRef, OnDestroy, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { NotificationService } from './service/notification.service'
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'
import { EventService } from '../shared/service/event.service'
import { AlertService } from '../shared/service/alert.service'
import { IUser } from '../user/model/user.model'
import { faBan, faPaperPlane } from '@fortawesome/free-solid-svg-icons'
import { MemberService } from '../member/service/member.service'
import { LanguageService } from '../shared/service/language.service'
import { AccountService } from '../account'
import { AlertMessage, AlertType, EventType } from '../app.constants'
import { ActivatedRoute, Router } from '@angular/router'
import { ReactiveFormsModule, FormsModule } from '@angular/forms'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { KeyValuePipe } from '@angular/common'

@Component({
  selector: 'app-send-notifications-dialog',
  templateUrl: './send-notifications-dialog.component.html',
  styleUrls: ['./send-notifications-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FormsModule, FaIconComponent, KeyValuePipe],
})
export class SendNotificationsDialogComponent implements OnInit {
  protected notificationService = inject(NotificationService)
  protected activeModal = inject(NgbActiveModal)
  protected eventService = inject(EventService)
  protected alertService = inject(AlertService)
  private languageService = inject(LanguageService)
  private memberService = inject(MemberService)
  private accountService = inject(AccountService)

  protected faPaperPlane = faPaperPlane
  protected faBan = faBan
  protected requestAlreadyInProgress = signal(false)
  protected languages = signal<{ [langCode: string]: { name: string } } | undefined>(undefined)
  protected language = signal('')
  protected account = signal<IUser | undefined>(undefined)

  ngOnInit() {
    this.languages.set(this.languageService.getAllLanguages())

    this.accountService.getAccountData().subscribe((account) => {
      this.memberService.find(account!.memberId).subscribe((member) => {
        if (member) {
          this.language.set(member.defaultLanguage || 'en')
        } else {
          this.language.set('en')
        }
      })
    })
  }

  clear() {
    this.activeModal.dismiss(true)
    window.history.back()
  }

  send() {
    this.notificationService.requestInProgress().subscribe((res: any) => {
      if (res.inProgress) {
        this.requestAlreadyInProgress.set(true)
      } else {
        this.notificationService.updateStatuses(this.language()).subscribe(() => {
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.NOTIFICATION_IN_PROGRESS)
          this.close()
        })
      }
    })
  }

  close() {
    this.eventService.broadcast({
      type: EventType.SEND_NOTIFICATIONS,
    })
    this.activeModal.dismiss(true)
  }
}

@Component({
  selector: 'app-send-notifications-popup',
  template: '',
})
export class SendNotificationsPopupComponent implements OnInit, OnDestroy {
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected modalService = inject(NgbModal)
  private destroyRef = inject(DestroyRef)

  protected ngbModalRef: NgbModalRef | undefined | null

  ngOnInit() {
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ assertion }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(SendNotificationsDialogComponent as Component, {
          size: 'lg',
          backdrop: 'static',
        })
        this.ngbModalRef.componentInstance.assertion = assertion
        this.ngbModalRef.result.then(
          (result) => {
            this.router.navigate(['/affiliations', { outlets: { popup: null } }])
            this.ngbModalRef = null
          },
          (reason) => {
            this.router.navigate(['/affiliations', { outlets: { popup: null } }])
            this.ngbModalRef = null
          }
        )
      }, 0)
    })
  }

  ngOnDestroy() {
    this.ngbModalRef = null
  }
}
