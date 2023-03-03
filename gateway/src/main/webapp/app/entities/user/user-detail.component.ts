import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { faTimesCircle, faCheckCircle } from '@fortawesome/free-solid-svg-icons';
import { IMSUser } from 'app/shared/model/user.model';
import { MSUserService } from './user.service';
import { JhiAlertService } from 'ng-jhipster';
import { MSMemberService } from '../member';
import { switchMap, tap } from 'rxjs/operators';

@Component({
  selector: 'jhi-ms-user-detail',
  templateUrl: './user-detail.component.html'
})
export class MSUserDetailComponent implements OnInit {
  msUser: IMSUser;
  faTimesCircle = faTimesCircle;
  faCheckCircle = faCheckCircle;
  DEFAULT_ADMIN = 'admin';
  superAdmin = false;

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected msUserService: MSUserService,
    protected jhiAlertService: JhiAlertService,
    protected memberService: MSMemberService
  ) {}

  ngOnInit() {
    this.activatedRoute.data
      .pipe(
        tap(({ msUser }) => {
          this.msUser = msUser;
        }),
        switchMap(({ msUser }) => this.memberService.find(msUser.salesforceId)),
        tap(member => {
          if (member && member.body) {
            this.superAdmin = member.body.superadminEnabled;
          }
        })
      )
      .subscribe();
  }

  sendActivate() {
    this.msUserService.sendActivate(this.msUser).subscribe(res => {
      if (res.ok) {
        this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.success.string', null, null);
      } else {
        this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.error.string', null, null);
      }
      this.previousState();
    });
  }

  isDefaultAdmin(msUser: IMSUser) {
    if (msUser.email === this.DEFAULT_ADMIN) {
      return true;
    }
    return false;
  }

  previousState() {
    window.history.back();
  }
}
