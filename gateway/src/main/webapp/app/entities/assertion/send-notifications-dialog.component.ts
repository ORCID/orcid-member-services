import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';

import { NotificationService } from 'app/shared/notification/notification.service';

import { faPaperPlane } from '@fortawesome/free-solid-svg-icons';
import { JhiLanguageHelper } from 'app/core/language/language.helper';
import { AccountService } from 'app/core';
import { Subscription } from 'rxjs';

@Component({
  selector: 'send-notifications-import-dialog',
  templateUrl: './send-notifications-dialog.component.html',
  providers: [NotificationService]
})
export class SendNotificationsDialogComponent implements OnInit {
  faPaperPlane = faPaperPlane;
  requestAlreadyInProgress = false;
  languages: string[];
  language: string;
  memberDataSubscription: Subscription;

  constructor(
    protected notificationService: NotificationService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager,
    protected jhiAlertService: JhiAlertService,
    private languageHelper: JhiLanguageHelper,
    private accountService: AccountService
  ) {}

  ngOnInit() {
    this.languageHelper.getAll().then(languages => {
      this.languages = languages;
    });
    this.memberDataSubscription = this.accountService.memberData.subscribe(data => {
      if (data) {
        this.language = data.defaultLanguage || 'en';
      } else {
        this.language = 'en';
      }
    });
  }

  ngOnDestroy() {
    this.memberDataSubscription.unsubscribe();
  }

  clear() {
    this.activeModal.dismiss(true);
    window.history.back();
  }

  send() {
    this.notificationService.requestInProgress().subscribe(res => {
      if (res.body.inProgress) {
        this.requestAlreadyInProgress = true;
      } else {
        this.notificationService.updateStatuses(this.language).subscribe(() => {
          this.accountService.updateDefaultLanguage(this.language);
          this.jhiAlertService.success('gatewayApp.assertionServiceAssertion.notifications.notificationInProgress.string', null, null);
          this.close();
        });
      }
    });
  }

  close() {
    this.eventManager.broadcast({
      name: 'sendNotifications',
      content: ''
    });
    this.activeModal.dismiss(true);
  }
}

@Component({
  selector: 'send-notifications-popup',
  template: ''
})
export class SendNotificationsPopupComponent implements OnInit, OnDestroy {
  protected ngbModalRef: NgbModalRef;

  constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ assertion }) => {
      setTimeout(() => {
        this.ngbModalRef = this.modalService.open(SendNotificationsDialogComponent as Component, { size: 'lg', backdrop: 'static' });
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
