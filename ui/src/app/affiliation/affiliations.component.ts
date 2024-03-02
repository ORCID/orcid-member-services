import { Component, OnDestroy, OnInit } from '@angular/core'
import { IAffiliation } from './model/affiliation.model'
import { AFFILIATION_STATUS } from '../shared/constants/orcid-api.constants'
import { Subscription, delay, tap } from 'rxjs'
import { EventType, ITEMS_PER_PAGE, ORCID_BASE_URL } from '../app.constants'
import {
  faChartBar,
  faFileDownload,
  faFileImport,
  faPaperPlane,
  faSearch,
  faTimes,
} from '@fortawesome/free-solid-svg-icons'
import { AffiliationService } from './service/affiliations.service'
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http'
import { LanguageService } from '../shared/service/language.service'
import { AccountService } from '../account'
import { AlertService } from '../shared/service/alert.service'
import { ActivatedRoute, Router } from '@angular/router'
import { EventService } from '../shared/service/event.service'

@Component({
  selector: 'app-affiliations',
  templateUrl: './affiliations.component.html',
  styleUrls: ['./affiliations.component.scss'],
})
export class AffiliationsComponent implements OnInit, OnDestroy {
  errorAddingToOrcid: string = AFFILIATION_STATUS.ERROR_ADDING_TO_ORCID
  errorUpdatingInOrcid: string = AFFILIATION_STATUS.ERROR_UPDATING_TO_ORCID
  errorDeletingInOrcid: string = AFFILIATION_STATUS.ERROR_DELETING_IN_ORCID
  currentAccount: any
  affiliations: IAffiliation[] | undefined
  error: any
  success: any
  eventSubscriber: Subscription | undefined
  importEventSubscriber: Subscription | undefined
  notificationSubscription: Subscription | undefined
  routeData: Subscription | undefined
  links: any
  totalItems: any
  itemsPerPage: any
  page: any
  predicate: any
  reverse: any
  orcidBaseUrl: string | undefined = ORCID_BASE_URL
  itemCount: string | undefined
  faChartBar = faChartBar
  faFileDownload = faFileDownload
  faFileImport = faFileImport
  faTimes = faTimes
  faSearch = faSearch
  faPaperPlane = faPaperPlane
  searchTerm: string | undefined
  submittedSearchTerm: string | undefined
  showEditReportPendingMessage: boolean | undefined
  showStatusReportPendingMessage: boolean | undefined
  showLinksReportPendingMessage: boolean | undefined
  paginationHeaderSubscription: Subscription | undefined

  constructor(
    protected affiliationService: AffiliationService,
    protected parseLinks: JhiParseLinks,
    protected alertService: AlertService,
    protected accountService: AccountService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventService: EventService,
    protected translate: LanguageService
  ) {
    this.itemsPerPage = ITEMS_PER_PAGE
  }

  ngOnInit() {
    this.accountService.identity().then((account) => {
      this.currentAccount = account
    })
    this.eventSubscriber = this.eventService.on(EventType.AFFILIATION_LIST_MODIFICATION).subscribe(() => {
      this.searchTerm = ''
      this.submittedSearchTerm = ''
      this.loadAll()
    })
    this.importEventSubscriber = this.eventService.on(EventType.IMPORT_AFFILIATIONS).subscribe(() => {
      this.loadAll()
    })
    this.notificationSubscription = this.eventService.on(EventType.SEND_NOTIFICATIONS).subscribe(() => {
      this.loadAll()
    })
    this.routeData = this.activatedRoute.data.subscribe((data) => {
      this.page = data.pagingParams.page
      this.reverse = data.pagingParams.ascending
      this.predicate = data.pagingParams.predicate
      this.loadAll()
    })
  }

  loadAll() {
    this.affiliationService
      .query({
        page: this.page - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
        filter: this.submittedSearchTerm ? this.submittedSearchTerm : '',
      })
      .subscribe(
        (res: IAffiliation[]) => this.paginateAssertions(res.body, res.headers),
        (res: HttpErrorResponse) => this.onError(res.message)
      )
  }

  transition() {
    this.router.navigate(['/assertion'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        sort: this.predicate + ',' + (this.reverse ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm ? this.submittedSearchTerm : '',
      },
    })
    this.loadAll()
  }

  clear() {
    this.page = 0
    this.router.navigate([
      '/assertion',
      {
        page: this.page,
        sort: this.predicate + ',' + (this.reverse ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm ? this.submittedSearchTerm : '',
      },
    ])
    this.loadAll()
  }

  trackId(index: number, item: IAffiliation) {
    return item.id
  }

  sort() {
    const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')]
    if (this.predicate !== 'id') {
      result.push('id')
    }
    return result
  }

  generatePermissionLinks() {
    this.affiliationService
      .generatePermissionLinks()
      .pipe(
        tap(() => (this.showLinksReportPendingMessage = true)),
        delay(10000),
        tap(() => (this.showLinksReportPendingMessage = false))
      )
      .subscribe((res) => {})
  }

  generateCSV() {
    this.affiliationService
      .generateCSV()
      .pipe(
        tap(() => (this.showEditReportPendingMessage = true)),
        delay(10000),
        tap(() => (this.showEditReportPendingMessage = false))
      )
      .subscribe((res) => {})
  }

  generateReport() {
    this.affiliationService
      .generateReport()
      .pipe(
        tap(() => (this.showStatusReportPendingMessage = true)),
        delay(10000),
        tap(() => (this.showStatusReportPendingMessage = false))
      )
      .subscribe((res) => {})
  }

  resetSearch() {
    this.page = 1
    this.searchTerm = ''
    this.submittedSearchTerm = ''
    this.loadAll()
  }

  submitSearch() {
    this.page = 1
    this.submittedSearchTerm = this.searchTerm
    this.loadAll()
  }

  protected paginateAssertions(data: IAffiliation[], headers: HttpHeaders) {
    this.links = this.parseLinks.parse(headers.get('link'))
    this.totalItems = parseInt(headers.get('X-Total-Count')!, 10)
    this.affiliations = data
    const first = (this.page - 1) * this.itemsPerPage === 0 ? 1 : (this.page - 1) * this.itemsPerPage + 1
    const second = this.page * this.itemsPerPage < this.totalItems ? this.page * this.itemsPerPage : this.totalItems
    this.paginationHeaderSubscription = this.translate
      .get('global.item-count.string', { first, second, total: this.totalItems })
      .subscribe((paginationHeader: string) => (this.itemCount = paginationHeader))
  }

  protected onError(errorMessage: string) {
    this.alertService.broadcast(errorMessage)
  }

  ngOnDestroy() {
    this.routeData!.unsubscribe()
    if (this.paginationHeaderSubscription) {
      this.paginationHeaderSubscription.unsubscribe()
    }
  }
}
