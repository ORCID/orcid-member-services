import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';
import { faChartBar, faFileDownload, faFileImport } from '@fortawesome/free-solid-svg-icons';

import { IAssertion } from 'app/shared/model/AssertionService/assertion.model';
import { AccountService } from 'app/core';

import { ITEMS_PER_PAGE } from 'app/shared';
import { AssertionService } from './assertion.service';
import { ORCID_BASE_URL } from 'app/app.constants';
import { ASSERTION_STATUS } from 'app/shared/constants/orcid-api.constants';

@Component({
  selector: 'jhi-assertion',
  templateUrl: './assertion.component.html',
  styleUrls: ['assertion.scss']
})
export class AssertionComponent implements OnInit, OnDestroy {
  errorAddingToOrcid: string = ASSERTION_STATUS.ERROR_ADDING_TO_ORCID;
  errorUpdatingInOrcid: string = ASSERTION_STATUS.ERROR_UPDATING_IN_ORCID;
  currentAccount: any;
  assertions: IAssertion[];
  error: any;
  success: any;
  eventSubscriber: Subscription;
  importEventSubscriber: Subscription;
  routeData: any;
  links: any;
  totalItems: any;
  itemsPerPage: any;
  page: any;
  predicate: any;
  previousPage: any;
  reverse: any;
  orcidBaseUrl: string = ORCID_BASE_URL;
  faChartBar = faChartBar;
  faFileDownload = faFileDownload;
  faFileImport = faFileImport;

  constructor(
    protected assertionService: AssertionService,
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

  ngOnInit() {
    this.loadAll();
    this.accountService.identity().then(account => {
      this.currentAccount = account;
    });
    this.eventSubscriber = this.eventManager.subscribe('assertionListModification', () => {
      this.loadAll();
    });
    this.importEventSubscriber = this.eventManager.subscribe('importAssertions', () => {
      this.loadAll();
      this.jhiAlertService.clear();
      this.jhiAlertService.success('gatewayApp.assertionServiceAssertion.import.success', null, null);
    });
  }

  loadAll() {
    this.assertionService
      .query({
        page: this.page - 1,
        size: this.itemsPerPage,
        sort: this.sort()
      })
      .subscribe(
        (res: HttpResponse<IAssertion[]>) => this.paginateAssertions(res.body, res.headers),
        (res: HttpErrorResponse) => this.onError(res.message)
      );
  }

  loadPage(page: number) {
    if (page !== this.previousPage) {
      this.previousPage = page;
      this.transition();
    }
  }

  transition() {
    this.router.navigate(['/assertion'], {
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
      '/assertion',
      {
        page: this.page,
        sort: this.predicate + ',' + (this.reverse ? 'asc' : 'desc')
      }
    ]);
    this.loadAll();
  }

  trackId(index: number, item: IAssertion) {
    return item.id;
  }

  sort() {
    const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  getLinks() {
    this.assertionService.getLinks();
  }

  getCSV() {
    this.assertionService.getCSV();
  }

  generateReport() {
    this.assertionService.generateReport();
  }

  protected paginateAssertions(data: IAssertion[], headers: HttpHeaders) {
    this.links = this.parseLinks.parse(headers.get('link'));
    this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
    this.assertions = data;
  }

  protected onError(errorMessage: string) {
    this.jhiAlertService.error(errorMessage, null, null);
  }

  ngOnDestroy() {
    this.eventManager.destroy(this.eventSubscriber);
  }
}
