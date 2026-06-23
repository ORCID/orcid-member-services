import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { IMember } from './model/member.model'
import { finalize } from 'rxjs'
import {
  faCheckCircle,
  faPencilAlt,
  faPlus,
  faSearch,
  faSortDown,
  faSortUp,
  faTimes,
  faTimesCircle,
} from '@fortawesome/free-solid-svg-icons'
import { MemberService } from './service/member.service'
import { ActivatedRoute, Router, RouterLink, RouterOutlet } from '@angular/router'
import { EventType, ITEMS_PER_PAGE } from '../app.constants'
import { AccountService } from '../account/service/account.service'
import { EventService } from '../shared/service/event.service'
import { Page } from '../shared/model/page.model'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { ReactiveFormsModule, FormsModule } from '@angular/forms'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { AlertComponent } from '../shared/alert/alert-toast.component'
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap'
import { DatePipe } from '@angular/common'

@Component({
  selector: 'app-members',
  templateUrl: './members.component.html',
  styleUrls: ['./members.component.scss'],
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
  ],
})
export class MembersComponent implements OnInit {
  protected memberService = inject(MemberService)
  protected accountService = inject(AccountService)
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected eventService = inject(EventService)
  private destroyRef = inject(DestroyRef)

  protected currentAccount = signal<any>(undefined)
  protected members = signal<IMember[] | undefined | null>(null)
  protected totalItems = signal<number>(0)
  protected itemsPerPage = signal<number>(ITEMS_PER_PAGE)
  protected page = signal(1)
  protected faTimesCircle = faTimesCircle
  protected faCheckCircle = faCheckCircle
  protected faTimes = faTimes
  protected faSearch = faSearch
  protected faSortDown = faSortDown
  protected faSortUp = faSortUp
  protected faPencilAlt = faPencilAlt
  protected faPlus = faPlus
  protected itemCount = signal<string | undefined>(undefined)
  protected searchTerm = signal<string>('')
  protected submittedSearchTerm = signal<string>('')
  protected sortColumn = signal('salesforceId')
  protected ascending = signal(true)
  protected isLoading = signal(false)

  ngOnInit() {
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data: any) => {
      this.page.set(data['queryParams'] ? data['queryParams'].page : 1)
      this.ascending.set(data['queryParams'] ? data['queryParams'].page.sort.split(',')[1] === 'asc' : true)
      this.sortColumn.set(data['queryParams'] ? data['queryParams'].page.sort.split(',')[0] : 'salesforceId')
    })
    this.loadAll()
    this.accountService.getAccountData().subscribe((account) => {
      this.currentAccount.set(account)
    })

    this.eventService.on(EventType.MEMBER_LIST_MODIFICATION)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.searchTerm.set('')
        this.submittedSearchTerm.set('')
        this.loadAll()
      })
  }

  loadAll() {
    if (this.submittedSearchTerm()) {
      this.searchTerm.set(this.submittedSearchTerm())
    } else {
      this.searchTerm.set('')
    }

    this.isLoading.set(true)
    this.memberService
      .query({
        page: this.page() - 1,
        size: this.itemsPerPage(),
        sort: this.sort(),
        filter: this.submittedSearchTerm() ? encodeURIComponent(this.submittedSearchTerm()) : '',
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

  loadPage() {
    this.router.navigate(['/members'], {
      queryParams: {
        page: this.page(),
        size: this.itemsPerPage(),
        sort: this.sortColumn() + ',' + (this.ascending() ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm() ? this.submittedSearchTerm() : '',
      },
    })
    this.loadAll()
  }

  clear() {
    this.page.set(0)
    this.router.navigate([
      '/members',
      {
        page: this.page(),
        sort: this.sortColumn() + ',' + (this.ascending() ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm() ? this.submittedSearchTerm() : '',
      },
    ])
    this.loadAll()
  }

  trackId(index: number, item: IMember) {
    return item.id
  }

  sort() {
    const result = [this.sortColumn() + ',' + (this.ascending() ? 'asc' : 'desc')]
    if (this.sortColumn() !== 'id') {
      result.push('id')
    }
    return result
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

  protected paginate(data: Page<IMember>) {
    this.totalItems.set(data.page.totalElements)
    this.members.set(data.content)

    if (this.totalItems() === 0) {
      this.itemCount.set($localize`:@@global.zero-item-count.string:Showing 0 - 0 of 0 items.`)
      return
    }

    // 2. Calculate the Start Index
    // Since page is 0-indexed, we take (0 * 20) + 1 = 1
    const first = data.page.number * data.page.size + 1

    // 3. Calculate the End Index
    // We calculate the theoretical end of the page, but cap it at totalItems using Math.min
    // e.g. Page 0, size 20: (0 + 1) * 20 = 20.
    // If totalItems is 15, Math.min(20, 15) returns 15.
    const calculatedEnd = (data.page.number + 1) * data.page.size
    const second = Math.min(calculatedEnd, data.page.totalElements)

    this.itemCount.set($localize`:@@global.item-count.string:Showing ${first} - ${second} of ${this.totalItems()} items.`)
  }

  updateSort(columnName: string) {
    if (this.sortColumn() && this.sortColumn() == columnName) {
      this.ascending.set(!this.ascending())
    } else {
      this.sortColumn.set(columnName)
    }
    this.loadPage()
  }
}
