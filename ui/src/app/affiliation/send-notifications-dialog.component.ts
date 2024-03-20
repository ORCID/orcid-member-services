import { Component, OnDestroy, OnInit } from '@angular/core'
import { NotificationService } from './service/notification.service'
import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap'
import { EventService } from '../shared/service/event.service'
import { AlertService } from '../shared/service/alert.service'
import { IUser } from '../user/model/user.model'
import { faPaperPlane } from '@fortawesome/free-solid-svg-icons'
import { IMember } from '../member/model/member.model'
import { MemberService } from '../member/service/member.service'
import { LanguageService } from '../shared/service/language.service'
import { AccountService } from '../account'
import { IAccount } from '../account/model/account.model'
import { AlertType, EventType } from '../app.constants'
import { ActivatedRoute, Router } from '@angular/router'

@Component({
  selector: 'app-send-notifications-dialog',
  templateUrl: './send-notifications-dialog.component.html',
  styleUrls: ['./send-notifications-dialog.component.scss'],
})
export class SendNotificationsDialogComponent implements OnInit {
  faPaperPlane = faPaperPlane
  requestAlreadyInProgress = false
  languages: { [langCode: string]: { name: string } } | undefined
  language: string = ''
  account: IUser | undefined

  constructor(
    protected notificationService: NotificationService,
    public activeModal: NgbActiveModal,
    protected eventService: EventService,
    protected alertService: AlertService,
    private languageService: LanguageService,
    private memberService: MemberService,
    private accountService: AccountService
  ) {}

  ngOnInit() {
    this.languages = this.languageService.getAllLanguages()

    this.accountService.getAccountData().subscribe((account) => {
      this.memberService.find(account!.salesforceId).subscribe((member) => {
        if (member) {
          this.language = member.defaultLanguage || 'en'
        } else {
          this.language = 'en'
        }
      })
    })
  }

  clear() {
    this.activeModal.dismiss(true)
    window.history.back()
  }

  send() {
    console.log('this.language is ', this.language)

    this.notificationService.requestInProgress().subscribe((res: any) => {
      if (res.inProgress) {
        this.requestAlreadyInProgress = true
      } else {
        this.notificationService.updateStatuses(this.language).subscribe(() => {
          this.alertService.broadcast(AlertType.NOTIFICATION_IN_PROGRESS)
          this.close()
        })
      }
    })
  }

  close() {
    this.eventService.broadcast({
      type: EventType.SEND_NOTIFICATIONS,
      payload: 'Send notifications',
    })
    this.activeModal.dismiss(true)
  }
}

@Component({
  selector: 'send-notifications-popup',
  template: '',
})
export class SendNotificationsPopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef | undefined | null

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal
  ) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ assertion }) => {
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
