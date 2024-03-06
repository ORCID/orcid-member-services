import { Component, OnDestroy, OnInit } from '@angular/core'
import { IAffiliation, IAffiliationPage } from './model/affiliation.model'
import { AFFILIATION_STATUS } from '../shared/constants/orcid-api.constants'
import { Subscription, delay, tap } from 'rxjs'
import { EventType, ITEMS_PER_PAGE, ORCID_BASE_URL } from '../app.constants'
import {
  faChartBar,
  faFileDownload,
  faFileImport,
  faPaperPlane,
  faPencilAlt,
  faPlus,
  faSearch,
  faSortDown,
  faSortUp,
  faTimes,
} from '@fortawesome/free-solid-svg-icons'
import { AffiliationService } from './service/affiliations.service'
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
  affiliations: IAffiliation[] | null | undefined
  error: any
  success: any
  eventSubscriber: Subscription | undefined
  importEventSubscriber: Subscription | undefined
  notificationSubscription: Subscription | undefined
  routeData: Subscription | undefined
  links: any
  totalItems: any
  itemsPerPage: any
  page = 1
  sortColumn = 'id'
  ascending: any
  orcidBaseUrl: string | undefined = ORCID_BASE_URL
  itemCount: string | undefined
  faChartBar = faChartBar
  faFileDownload = faFileDownload
  faFileImport = faFileImport
  faTimes = faTimes
  faSearch = faSearch
  faPaperPlane = faPaperPlane
  faSortDown = faSortDown
  faSortUp = faSortUp
  faPencilAlt = faPencilAlt
  faPlus = faPlus
  searchTerm: string | undefined
  submittedSearchTerm: string | undefined
  showEditReportPendingMessage: boolean | undefined
  showStatusReportPendingMessage: boolean | undefined
  showLinksReportPendingMessage: boolean | undefined
  paginationHeaderSubscription: Subscription | undefined

  constructor(
    protected affiliationService: AffiliationService,
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
    this.accountService.getAccountData().subscribe((account) => {
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
      this.page = data['queryParams'] ? data['queryParams'].page : 1
      this.ascending = data['queryParams'] ? data['queryParams'].page.sort.split(',')[1] : false
      this.sortColumn = data['queryParams'] ? data['queryParams'].page.sort.split(',')[0] : 'id'
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
      .subscribe({
        next: (res) => {
          if (res) {
            this.paginate(res)
          }
        },
      })
  }

  updateSort(columnName: string) {
    if (this.sortColumn && this.sortColumn == columnName) {
      this.ascending = !this.ascending
    } else {
      this.sortColumn = columnName
    }
    this.loadPage()
  }

  loadPage() {
    this.router.navigate(['/affiliations'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        sort: this.sortColumn + ',' + (this.ascending ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm ? this.submittedSearchTerm : '',
      },
    })
    this.loadAll()
  }

  clear() {
    this.page = 0
    this.router.navigate([
      '/affiliations',
      {
        page: this.page,
        sort: this.sortColumn + ',' + (this.ascending ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm ? this.submittedSearchTerm : '',
      },
    ])
    this.loadAll()
  }

  trackId(index: number, item: IAffiliation) {
    return item.id
  }

  sort() {
    const result = [this.sortColumn + ',' + (this.ascending ? 'asc' : 'desc')]
    if (this.sortColumn !== 'id') {
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

  protected paginate(res: IAffiliationPage) {
    this.totalItems = res.totalItems
    this.affiliations = res.affiliations
    const first = (this.page - 1) * this.itemsPerPage === 0 ? 1 : (this.page - 1) * this.itemsPerPage + 1
    const second = this.page * this.itemsPerPage < this.totalItems ? this.page * this.itemsPerPage : this.totalItems
    this.itemCount = $localize`:@@global.item-count.string:Showing ${first} - ${second} of ${this.totalItems} items.`
  }

  protected onError(errorMessage: string) {
    this.alertService.broadcast(errorMessage)
  }

  ngOnDestroy() {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe()
    }
    if (this.eventSubscriber) {
      this.eventSubscriber.unsubscribe()
    }
    if (this.importEventSubscriber) {
      this.importEventSubscriber.unsubscribe()
    }
  }
}
