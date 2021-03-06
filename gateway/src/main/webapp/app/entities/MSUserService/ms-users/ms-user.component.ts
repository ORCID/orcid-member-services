import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { JhiAlertService, JhiEventManager, JhiParseLinks } from 'ng-jhipster';
import { faCheckCircle, faTimesCircle } from '@fortawesome/free-solid-svg-icons';

import { IMSUser } from 'app/shared/model/MSUserService/ms-user.model';
import { AccountService } from 'app/core';
import { MSMemberService } from 'app/entities/MSUserService/ms-members/ms-member.service';
import { SERVER_API_URL } from 'app/app.constants';
import { ITEMS_PER_PAGE } from 'app/shared';
import { MSUserService } from './ms-user.service';

import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'jhi-ms-user',
  templateUrl: './ms-user.component.html'
})
export class MSUserComponent implements OnInit, OnDestroy {
  currentAccount: IMSUser;
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
  itemCount: string;

  faTimesCircle = faTimesCircle;
  faCheckCircle = faCheckCircle;
  DEFAULT_ADMIN = 'admin@orcid.org';

  constructor(
    protected msUserService: MSUserService,
    protected msMemberService: MSMemberService,
    protected parseLinks: JhiParseLinks,
    protected jhiAlertService: JhiAlertService,
    protected accountService: AccountService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventManager: JhiEventManager,
    protected translate: TranslateService
  ) {
    this.itemsPerPage = ITEMS_PER_PAGE;
    this.routeData = this.activatedRoute.data.subscribe(data => {
      this.page = data.pagingParams.page;
      this.previousPage = data.pagingParams.page;
      this.reverse = data.pagingParams.ascending;
      this.predicate = data.pagingParams.predicate;
    });
  }

  ngOnInit() {
    this.msMemberService.getOrgNameMap();
    this.accountService.identity().then(account => {
      this.currentAccount = account;
    });
    this.loadAll();
    this.eventSubscriber = this.eventManager.subscribe('msUserListModification', () => {
      this.loadAll();
    });
  }

  generateItemCountString() {
    const first = (this.page - 1) * this.itemsPerPage == 0 ? 1 : (this.page - 1) * this.itemsPerPage + 1;
    const second = this.page * this.itemsPerPage < this.totalItems ? this.page * this.itemsPerPage : this.totalItems;
    return this.translate.instant('global.item-count.string', { first: first, second: second, total: this.totalItems });
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

  trackId(index: number, item: IMSUser) {
    return item.id;
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
        this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.success.string', null, null);
      } else {
        this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.error.string', null, null);
      }
    });
  }

  disableDelete(msUser: IMSUser) {
    if (msUser.login === this.DEFAULT_ADMIN) {
      return true;
    }
    if (msUser.mainContact) {
      return true;
    }
    return msUser.login === this.currentAccount.login;
  }

  isDefaultAdmin(msUser: IMSUser) {
    return msUser.login === this.DEFAULT_ADMIN;
  }

  isUserLoggedIn(msUser: IMSUser) {
    return msUser.login === this.currentAccount.login;
  }

  disableImpersonate(msUser: IMSUser) {
    if (msUser.login === this.DEFAULT_ADMIN) {
      return true;
    }
    return msUser.login === this.currentAccount.login;
  }

  hasRoleAdmin() {
    return this.accountService.hasAnyAuthority(['ROLE_ADMIN']);
  }

  isOrganizationOwner() {
    return this.accountService.isOrganizationOwner();
  }

  switchUser(login: string) {
    this.msUserService.switchUser(login).subscribe(res => {
      window.location.href = SERVER_API_URL;
    });
  }

  protected paginateMSUser(data: IMSUser[], headers: HttpHeaders) {
    this.links = this.parseLinks.parse(headers.get('link'));
    this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
    this.msUser = data;
    this.itemCount = this.generateItemCountString();
  }

  protected onError(errorMessage: string) {
    this.jhiAlertService.error(errorMessage, null, null);
  }

  ngOnDestroy() {
    this.eventManager.destroy(this.eventSubscriber);
  }
}
