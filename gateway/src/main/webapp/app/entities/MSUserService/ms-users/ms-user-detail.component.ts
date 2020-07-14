import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { faTimesCircle, faCheckCircle } from '@fortawesome/free-solid-svg-icons';
import { IMSUser } from 'app/shared/model/MSUserService/ms-user.model';
import { MSMemberService } from 'app/entities/MSUserService/ms-members/ms-member.service';
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

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected msUserService: MSUserService,
    protected jhiAlertService: JhiAlertService,
    protected msMemberService: MSMemberService
  ) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ msUser }) => {
      this.msUser = msUser;
    });
    this.msMemberService.getOrgNameMap();
  }

  sendActivate() {
    this.msUserService.sendActivate(this.msUser).subscribe(res => {
      if (res.ok) {
        this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.success', null, null);
      } else {
        this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.error', null, null);
      }
      this.previousState();
    });
  }

  previousState() {
    window.history.back();
  }
}
