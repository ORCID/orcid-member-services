import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink, RouterOutlet } from '@angular/router'
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
import { delay, finalize, tap } from 'rxjs'
import { AccountService } from '../account'
import { EventType, ITEMS_PER_PAGE, ORCID_BASE_URL } from '../app.constants'
import { AFFILIATION_STATUS } from '../shared/constants/orcid-api.constants'
import { Page } from '../shared/model/page.model'
import { AlertService } from '../shared/service/alert.service'
import { DateUtilService } from '../shared/service/date-util.service'
import { EventService } from '../shared/service/event.service'
import { LanguageService } from '../shared/service/language.service'
import { IAffiliation } from './model/affiliation.model'
import { AffiliationService } from './service/affiliation.service'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { ReactiveFormsModule, FormsModule } from '@angular/forms'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { AlertComponent } from '../shared/alert/alert-toast.component'
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap'
import { DatePipe } from '@angular/common'
import { LocalizePipe } from '../shared/pipe/localize'

@Component({
  selector: 'app-affiliations',
  templateUrl: './affiliations.component.html',
  styleUrls: ['./affiliations.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    FaIconComponent,
    ReactiveFormsModule,
    FormsModule,
    ErrorAlertComponent,
    AlertComponent,
    NgbPaginationModule,
    RouterOutlet,
    DatePipe,
    LocalizePipe,
  ],
})
export class AffiliationsComponent implements OnInit {
  protected affiliationService = inject(AffiliationService)
  protected alertService = inject(AlertService)
  protected accountService = inject(AccountService)
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected eventService = inject(EventService)
  protected translate = inject(LanguageService)
  protected dateUtilService = inject(DateUtilService)
  private destroyRef = inject(DestroyRef)

  protected errorAddingToOrcid: string = AFFILIATION_STATUS.ERROR_ADDING_TO_ORCID
  protected errorUpdatingInOrcid: string = AFFILIATION_STATUS.ERROR_UPDATING_TO_ORCID
  protected errorDeletingInOrcid: string = AFFILIATION_STATUS.ERROR_DELETING_IN_ORCID
  protected currentAccount = signal<any>(undefined)
  protected affiliations = signal<IAffiliation[] | null | undefined>(null)
  success: any
  protected totalItems = signal<number>(0)
  protected itemsPerPage = signal<number>(ITEMS_PER_PAGE)
  protected page = signal(1)
  protected sortColumn = signal('id')
  protected ascending = signal(true)
  protected orcidBaseUrl: string | undefined = ORCID_BASE_URL
  protected itemCount = signal<string | undefined>(undefined)
  startDate: string | undefined
  endDate: string | undefined
  protected faChartBar = faChartBar
  protected faFileDownload = faFileDownload
  protected faFileImport = faFileImport
  protected faTimes = faTimes
  protected faSearch = faSearch
  protected faPaperPlane = faPaperPlane
  protected faSortDown = faSortDown
  protected faSortUp = faSortUp
  protected faPencilAlt = faPencilAlt
  protected faPlus = faPlus
  protected searchTerm = signal<string>('')
  protected submittedSearchTerm = signal<string>('')
  protected showEditReportPendingMessage = signal<boolean>(false)
  protected showStatusReportPendingMessage = signal<boolean>(false)
  protected showLinksReportPendingMessage = signal<boolean>(false)
  protected isLoading = signal(false)

  ngOnInit() {
    this.accountService.getAccountData().subscribe((account) => {
      this.currentAccount.set(account)
    })
    this.eventService.on(EventType.AFFILIATION_LIST_MODIFICATION)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.searchTerm.set('')
        this.submittedSearchTerm.set('')
        this.loadAll()
      })
    this.eventService.on(EventType.IMPORT_AFFILIATIONS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.loadAll()
      })
    this.eventService.on(EventType.SEND_NOTIFICATIONS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.loadAll()
      })
    this.activatedRoute.data
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => {
        this.page.set(data['queryParams'] ? data['queryParams'].page : 1)
        this.ascending.set(data['queryParams'] ? data['queryParams'].page.sort.split(',')[1] === 'asc' : true)
        this.sortColumn.set(data['queryParams'] ? data['queryParams'].page.sort.split(',')[0] : 'email')
        this.loadAll()
      })
  }

  loadAll() {
    this.isLoading.set(true)
    this.affiliationService
      .query({
        page: this.page() - 1,
        size: this.itemsPerPage(),
        sort: this.sort(),
        filter: this.submittedSearchTerm() ? this.submittedSearchTerm() : '',
      })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (res) => {
          if (res) {
            this.paginate(res)
          }
        },
      })
  }

  updateSort(columnName: string) {
    if (this.sortColumn() && this.sortColumn() == columnName) {
      this.ascending.set(!this.ascending())
    } else {
      this.sortColumn.set(columnName)
    }
    this.loadPage()
  }

  loadPage() {
    this.router.navigate(['/affiliations'], {
      queryParams: {
        page: this.page(),
        size: this.itemsPerPage(),
        sort: this.sortColumn() + ',' + (this.ascending() ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm() ? this.submittedSearchTerm() : '',
      },
    })
    this.loadAll()
  }

  formatDate(year?: string, month?: string, day?: string) {
    return this.dateUtilService.formatDate({
      year,
      month,
      day,
    })
  }

  clear() {
    this.page.set(0)
    this.router.navigate([
      '/affiliations',
      {
        page: this.page(),
        sort: this.sortColumn() + ',' + (this.ascending() ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm() ? this.submittedSearchTerm() : '',
      },
    ])
    this.loadAll()
  }

  trackId(index: number, item: IAffiliation) {
    return item.id
  }

  sort() {
    const result = [this.sortColumn() + ',' + (this.ascending() ? 'asc' : 'desc')]
    if (this.sortColumn() !== 'id') {
      result.push('id')
    }
    return result
  }

  generatePermissionLinks() {
    this.affiliationService
      .generatePermissionLinks()
      .pipe(
        tap(() => this.showLinksReportPendingMessage.set(true)),
        delay(10000),
        tap(() => this.showLinksReportPendingMessage.set(false))
      )
      .subscribe()
  }

  generateCSV() {
    this.affiliationService
      .generateCSV()
      .pipe(
        tap(() => this.showEditReportPendingMessage.set(true)),
        delay(10000),
        tap(() => this.showEditReportPendingMessage.set(false))
      )
      .subscribe()
  }

  generateReport() {
    this.affiliationService
      .generateReport()
      .pipe(
        tap(() => this.showStatusReportPendingMessage.set(true)),
        delay(10000),
        tap(() => this.showStatusReportPendingMessage.set(false))
      )
      .subscribe()
  }

  resetSearch() {
    this.page.set(1)
    this.searchTerm.set('')
    this.submittedSearchTerm.set('')
    this.loadAll()
  }

  submitSearch() {
    this.page.set(1)
    this.submittedSearchTerm.set(this.searchTerm())
    this.loadAll()
  }

  protected paginate(data: Page<IAffiliation>) {
    this.totalItems.set(data.page.totalElements)
    this.affiliations.set(data.content)

    if (this.totalItems() === 0) {
      this.itemCount.set($localize`:@@global.zero-item-count.string:Showing 0 - 0 of 0 items.`)
      return
    }

    const first = data.page.number * data.page.size + 1
    const calculatedEnd = (data.page.number + 1) * data.page.size
    const second = Math.min(calculatedEnd, data.page.totalElements)
    this.itemCount.set($localize`:@@global.item-count.string:Showing ${first} - ${second} of ${this.totalItems()} items.`)
  }
}
