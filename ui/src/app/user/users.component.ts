import { Component, OnDestroy, OnInit } from '@angular/core'
import { IUser } from './model/user.model'
import { Subscription, filter } from 'rxjs'
import { UserService } from './service/user.service'
import { HttpErrorResponse } from '@angular/common/http'
import { IUserPage, UserPage } from './model/user-page.model'
import { faCheckCircle, faSearch, faTimes, faTimesCircle } from '@fortawesome/free-solid-svg-icons'
import { EventType, ITEMS_PER_PAGE } from '../app.constants'
import { ActivatedRoute, Router } from '@angular/router'
import { AccountService } from '../account/service/account.service'
import { EventService } from '../shared/service/event.service'
import { IAccount } from '../account/model/account.model'

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
  page: any
  predicate: any
  reverse: any
  itemCount: string | null | undefined = null
  searchTerm: string | null = null
  submittedSearchTerm: string | null = null
  paginationHeaderSubscription: Subscription | null = null

  faTimesCircle = faTimesCircle
  faCheckCircle = faCheckCircle
  faTimes = faTimes
  faSearch = faSearch

  DEFAULT_ADMIN = 'admin@orcid.org'

  constructor(
    protected userService: UserService,
    // protected jhiAlertService: JhiAlertService,
    protected accountService: AccountService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventService: EventService //protected translate: TranslateService
  ) {
    this.itemsPerPage = ITEMS_PER_PAGE
    this.routeData = this.activatedRoute.data.subscribe((data) => {
      console.log(data)

      /* this.page = data['pagingParams'].page
      this.reverse = data['pagingParams'].ascending
      this.predicate = data['pagingParams'].predicate */
      this.page = 1
      this.reverse = true
      this.predicate = 'id'
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
          error: (res: HttpErrorResponse) => this.onError(res.message),
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
          error: (res: HttpErrorResponse) => this.onError(res.message),
        })
    }
  }

  loadPage(page: number) {
    this.transition()
  }

  transition() {
    this.router.navigate(['/users'], {
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
      '/users',
      {
        page: this.page,
        sort: this.predicate + ',' + (this.reverse ? 'asc' : 'desc'),
        filter: this.submittedSearchTerm ? this.submittedSearchTerm : '',
      },
    ])
    this.loadAll()
  }

  trackId(index: number, item: IUser) {
    return item.id
  }

  sort() {
    const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')]
    if (this.predicate !== 'id') {
      result.push('id')
    }
    return result
  }

  sendActivate(msUser: IUser) {
    this.userService.sendActivate(msUser).subscribe((res) => {
      if (res) {
        //this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.success.string', null, null)
      } else {
        //this.jhiAlertService.success('gatewayApp.msUserServiceMSUser.sendActivate.error.string', null, null)
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

  protected onError(errorMessage: string) {
    //this.jhiAlertService.error(errorMessage, null, null)
  }

  ngOnDestroy() {
    this.eventSubscriber?.unsubscribe()
  }
}
