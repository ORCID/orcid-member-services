import { Component, OnDestroy, OnInit } from '@angular/core'
import { IUser } from './model/user.model'
import { Subscription, filter } from 'rxjs'
import { UserService } from './service/user.service'
import { HttpErrorResponse } from '@angular/common/http'
import { IUserPage, UserPage } from './model/user-page.model'
import {
  faCheckCircle,
  faPencilAlt,
  faPlus,
  faSearch,
  faSignInAlt,
  faTimes,
  faTimesCircle,
  faSortDown,
  faSortUp,
} from '@fortawesome/free-solid-svg-icons'
import { AlertType, EventType, ITEMS_PER_PAGE } from '../app.constants'
import { ActivatedRoute, Router } from '@angular/router'
import { AccountService } from '../account/service/account.service'
import { EventService } from '../shared/service/event.service'
import { IAccount } from '../account/model/account.model'
import { AlertService } from '../shared/service/alert.service'

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss'],
})
export class UsersComponent implements OnInit, OnDestroy {
  currentAccount: IAccount | undefined
  users: IUser[] | null | undefined
  error: any
  eventSubscriber: Subscription | null = null
  routeData: any
  links: any
  totalItems: any
  itemsPerPage: any
  page = 1
  sortColumn = 'id'
  ascending: any
  itemCount: string | null | undefined = null
  searchTerm: string | null = null
  submittedSearchTerm: string | null = null
  paginationHeaderSubscription: Subscription | null = null

  faTimesCircle = faTimesCircle
  faCheckCircle = faCheckCircle
  faTimes = faTimes
  faSearch = faSearch
  faPlus = faPlus
  faPencilAlt = faPencilAlt
  faSignInAlt = faSignInAlt
  faSortDown = faSortDown
  faSortUp = faSortUp
  DEFAULT_ADMIN = 'admin@orcid.org'

  constructor(
    protected userService: UserService,
    protected alertService: AlertService,
    protected accountService: AccountService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventService: EventService
  ) {
    this.itemsPerPage = ITEMS_PER_PAGE
    this.routeData = this.activatedRoute.data.subscribe((data) => {
      this.page = data['queryParams'] ? data['queryParams'].page : 1
      this.ascending = data['queryParams'] ? data['queryParams'].page.sort.split(',')[1] : false
      this.sortColumn = data['queryParams'] ? data['queryParams'].page.sort.split(',')[0] : 'id'
    })
  }

  ngOnInit() {
    this.accountService.getAccountData().subscribe((account) => {
      if (account) {
        this.currentAccount = account
      }
    })
    this.loadAll()

    this.eventSubscriber = this.eventService.on(EventType.USER_LIST_MODIFIED).subscribe(() => {
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

    if (this.hasRoleAdmin()) {
      this.userService
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
    } else {
      this.userService
        .findBySalesForceId(this.accountService.getSalesforceId(), {
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
    this.router.navigate(['/users'], {
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
      '/users',
      {
        page: this.page,
        sort: this.sortColumn + ',' + (this.ascending ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm ? this.submittedSearchTerm : '',
      },
    ])
    this.loadAll()
  }

  sort() {
    const result = [this.sortColumn + ',' + (this.ascending ? 'asc' : 'desc')]
    if (this.sortColumn !== 'id') {
      result.push('id')
    }
    return result
  }

  sendActivate(msUser: IUser) {
    this.userService.sendActivate(msUser).subscribe((res) => {
      if (res) {
        this.alertService.broadcast(AlertType.SEND_ACTIVATION_SUCCESS)
      } else {
        this.alertService.broadcast(AlertType.SEND_ACTIVATION_FAILURE)
      }
    })
  }

  disableDelete(msUser: IUser) {
    if (msUser.email === this.DEFAULT_ADMIN) {
      return true
    }
    if (msUser.mainContact) {
      return true
    }
    return msUser.email === this.currentAccount?.email
  }

  isDefaultAdmin(msUser: IUser) {
    return msUser.email === this.DEFAULT_ADMIN
  }

  isUserLoggedIn(msUser: IUser) {
    return msUser.email === this.currentAccount?.email
  }

  disableImpersonate(msUser: IUser) {
    if (msUser.email === this.DEFAULT_ADMIN) {
      return true
    }
    return msUser.email === this.currentAccount?.email
  }

  hasRoleAdmin() {
    return this.accountService.hasAnyAuthority(['ROLE_ADMIN'])
  }

  isOrganizationOwner() {
    return this.accountService.isOrganizationOwner()
  }

  switchUser(login: string | undefined) {
    if (login) {
      this.userService.switchUser(login).subscribe((res) => {
        // TODO: what is this doing? useless? revisit when working on impersonation
        // window.location.href = SERVER_API_URL
      })
    }
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

  protected paginate(data: IUserPage) {
    this.totalItems = data.totalItems
    this.users = data.users
    const first = (this.page - 1) * this.itemsPerPage === 0 ? 1 : (this.page - 1) * this.itemsPerPage + 1
    const second = this.page * this.itemsPerPage < this.totalItems ? this.page * this.itemsPerPage : this.totalItems
    this.itemCount = $localize`:@@global.item-count.string:Showing ${first} - ${second} of ${this.totalItems} items.`
  }

  ngOnDestroy() {
    this.eventSubscriber?.unsubscribe()
  }
}
