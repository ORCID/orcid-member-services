import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';
import { faTimesCircle, faCheckCircle } from '@fortawesome/free-solid-svg-icons';

import { IMSUser } from 'app/shared/model/MSUserService/ms-user.model';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/MSUserService/ms-members/ms-member.service';

import { ITEMS_PER_PAGE } from 'app/shared';
import { MSUserService } from './ms-user.service';

@Component({
  selector: 'jhi-ms-user',
  templateUrl: './ms-user.component.html'
})
export class MSUserComponent implements OnInit, OnDestroy {
  currentAccount: any;
  msUser: IMSUser[];
  error: any;
  success: any;
  eventSubscriber: Subscription;
  routeData: any;
  links: any;
  totalItems: any;
  itemsPerPage: any;
  page: any;
  predicate: any;
  previousPage: any;
  reverse: any;

  faTimesCircle = faTimesCircle;
  faCheckCircle = faCheckCircle;
  DEFAULT_ADMIN = 'admin';

  constructor(
    protected msUserService: MSUserService,
    protected msMemberService: MSMemberService,
    protected parseLinks: JhiParseLinks,
    protected jhiAlertService: JhiAlertService,
    protected accountService: AccountService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventManager: JhiEventManager
  ) {
    this.itemsPerPage = ITEMS_PER_PAGE;
    this.routeData = this.activatedRoute.data.subscribe(data => {
      this.page = data.pagingParams.page;
      this.previousPage = data.pagingParams.page;
      this.reverse = data.pagingParams.ascending;
      this.predicate = data.pagingParams.predicate;
    });
  }

  loadAll() {
    if (this.hasRoleAdmin()) {
      this.msUserService
        .query({
          page: this.page - 1,
          size: this.itemsPerPage,
          sort: this.sort()
        })
        .subscribe(
          (res: HttpResponse<IMSUser[]>) => this.paginateMSUser(res.body, res.headers),
          (res: HttpErrorResponse) => this.onError(res.message)
        );
    } else {
      this.msUserService
        .findBySalesForceId(this.accountService.getSalesforceId(), {
          page: this.page - 1,
          size: this.itemsPerPage,
          sort: this.sort()
        })
        .subscribe(
          (res: HttpResponse<IMSUser[]>) => this.paginateMSUser(res.body, res.headers),
          (res: HttpErrorResponse) => this.onError(res.message)
        );
    }
  }

  loadPage(page: number) {
    if (page !== this.previousPage) {
      this.previousPage = page;
      this.transition();
    }
  }

  transition() {
    this.router.navigate(['/ms-user'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        sort: this.predicate + ',' + (this.reverse ? 'asc' : 'desc')
      }
    });
    this.loadAll();
  }

  clear() {
    this.page = 0;
    this.router.navigate([
      '/ms-user',
      {
        page: this.page,
        sort: this.predicate + ',' + (this.reverse ? 'asc' : 'desc')
      }
    ]);
    this.loadAll();
  }

  ngOnInit() {
    this.accountService.identity().then(account => {
      this.currentAccount = account;
    });
    this.loadAll();
    this.registerChangeInMSUser();
    this.msMemberService.getOrgNameMap();
  }

  ngOnDestroy() {
    this.eventManager.destroy(this.eventSubscriber);
  }

  trackId(index: number, item: IMSUser) {
    return item.id;
  }

  registerChangeInMSUser() {
    this.eventSubscriber = this.eventManager.subscribe('msUserListModification', response => this.loadAll());
  }

  sort() {
    const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  sendActivate(msUser: IMSUser) {
    this.msUserService.sendActivate(msUser).subscribe(res => {
      if (res.ok) {
        this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.success', null, null);
      } else {
        this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.error', null, null);
      }
    });
  }

  switchUser(msUser: IMSUser) {
    this.msUserService.switchUser(msUser).subscribe(res => {
      this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.success', null, null);
      console.log('response', res);
    });
  }

  isDefaultAdmin(msUser: IMSUser) {
    if (msUser.login == this.DEFAULT_ADMIN) {
      return true;
    }
    return false;
  }

  hasRoleAdmin() {
    return this.accountService.hasAnyAuthority(['ROLE_ADMIN']);
  }

  isOrganizationOwner() {
    return this.accountService.isOrganizationOwner();
  }

  protected paginateMSUser(data: IMSUser[], headers: HttpHeaders) {
    this.links = this.parseLinks.parse(headers.get('link'));
    this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
    this.msUser = data;
  }

  protected onError(errorMessage: string) {
    this.jhiAlertService.error(errorMessage, null, null);
  }
}
