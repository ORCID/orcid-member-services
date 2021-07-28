import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { faTimesCircle, faCheckCircle } from '@fortawesome/free-solid-svg-icons';
import { IMSUser } from 'app/shared/model/MSUserService/ms-user.model';
import { MSUserService } from './ms-user.service';
import { JhiAlertService } from 'ng-jhipster';

@Component({
  selector: 'jhi-ms-user-detail',
  templateUrl: './ms-user-detail.component.html'
})
export class MSUserDetailComponent implements OnInit {
  msUser: IMSUser;
  faTimesCircle = faTimesCircle;
  faCheckCircle = faCheckCircle;
  DEFAULT_ADMIN = 'admin';

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected msUserService: MSUserService,
    protected jhiAlertService: JhiAlertService
  ) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ msUser }) => {
      this.msUser = msUser;
    });
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
    if (msUser.login == this.DEFAULT_ADMIN) {
      return true;
    }
    return false;
  }

  previousState() {
    window.history.back();
  }
}
