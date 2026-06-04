import { Component, OnInit } from '@angular/core'
import { IMember } from './model/member.model'
import { Subscription } from 'rxjs'
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
import { ActivatedRoute, Router } from '@angular/router'
import { EventType, ITEMS_PER_PAGE } from '../app.constants'
import { AccountService } from '../account/service/account.service'
import { EventService } from '../shared/service/event.service'
import { Page } from '../shared/model/page.model'

@Component({
    selector: 'app-members',
    templateUrl: './members.component.html',
    standalone: false
})
export class MembersComponent implements OnInit {
  currentAccount: any
  members: IMember[] | undefined | null
  eventSubscriber: Subscription | undefined
  routeData: any
  links: any
  totalItems: any
  itemsPerPage: any
  page = 1
  predicate: any
  reverse: any
  faTimesCircle = faTimesCircle
  faCheckCircle = faCheckCircle
  faTimes = faTimes
  faSearch = faSearch
  faSortDown = faSortDown
  faSortUp = faSortUp
  faPencilAlt = faPencilAlt
  faPlus = faPlus
  itemCount: string | undefined
  searchTerm: string | undefined
  submittedSearchTerm: string | undefined
  paginationHeaderSubscription: Subscription | undefined
  sortColumn = 'salesforceId'
  ascending: any

  constructor(
    protected memberService: MemberService,
    protected accountService: AccountService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventService: EventService
  ) {
    this.itemsPerPage = ITEMS_PER_PAGE
    this.routeData = this.activatedRoute.data.subscribe((data: any) => {
      this.page = data['queryParams'] ? data['queryParams'].page : 1
      this.ascending = data['queryParams'] ? data['queryParams'].page.sort.split(',')[1] : true
      this.sortColumn = data['queryParams'] ? data['queryParams'].page.sort.split(',')[0] : 'salesforceId'
    })
  }

  ngOnInit() {
    this.loadAll()
    this.accountService.getAccountData().subscribe((account) => {
      this.currentAccount = account
    })

    this.eventSubscriber = this.eventService.on(EventType.MEMBER_LIST_MODIFICATION).subscribe(() => {
      this.searchTerm = ''
      this.submittedSearchTerm = ''
      this.loadAll()
    })
  }

  loadAll() {
    if (this.submittedSearchTerm) {
      this.searchTerm = this.submittedSearchTerm
    } else {
      this.searchTerm = ''
    }

    this.memberService
      .query({
        page: this.page - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
        filter: this.submittedSearchTerm ? encodeURIComponent(this.submittedSearchTerm) : '',
      })
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
      '/members',
      {
        page: this.page,
        sort: this.predicate + ',' + (this.reverse ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm ? this.submittedSearchTerm : '',
      },
    ])
    this.loadAll()
  }

  trackId(index: number, item: IMember) {
    return item.id
  }

  sort() {
    const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')]
    if (this.predicate !== 'id') {
      result.push('id')
    }
    return result
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

  protected paginate(data: Page<IMember>) {
    this.totalItems = data.page.totalElements
    this.members = data.content

    if (this.totalItems === 0) {
      this.itemCount = $localize`:@@global.zero-item-count.string:Showing 0 - 0 of 0 items.`
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

    this.itemCount = $localize`:@@global.item-count.string:Showing ${first} - ${second} of ${this.totalItems} items.`
  }

  updateSort(columnName: string) {
    if (this.sortColumn && this.sortColumn == columnName) {
      this.ascending = !this.ascending
    } else {
      this.sortColumn = columnName
    }
    this.loadPage()
  }
}
