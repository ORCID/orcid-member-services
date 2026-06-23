import { ChangeDetectionStrategy, Component, NgZone, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { IUser, User } from './model/user.model'
import { finalize } from 'rxjs'
import { UserService } from './service/user.service'
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
import { AlertMessage, AlertType, EventType, ITEMS_PER_PAGE } from '../app.constants'
import { ActivatedRoute, Router, RouterLink, RouterOutlet } from '@angular/router'
import { AccountService } from '../account/service/account.service'
import { EventService } from '../shared/service/event.service'
import { IAccount } from '../account/model/account.model'
import { AlertService } from '../shared/service/alert.service'
import { Page } from '../shared/model/page.model'
import { FeatureToggleService } from '../shared/service/feature-toggle.service'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { HasAnyAuthorityDirective } from '../shared/directive/has-any-authority.directive'
import { ReactiveFormsModule, FormsModule } from '@angular/forms'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { AlertComponent } from '../shared/alert/alert-toast.component'
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap'
import { DatePipe } from '@angular/common'

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    FaIconComponent,
    HasAnyAuthorityDirective,
    ReactiveFormsModule,
    FormsModule,
    ErrorAlertComponent,
    AlertComponent,
    NgbPaginationModule,
    RouterOutlet,
    DatePipe,
  ],
})
export class UsersComponent implements OnInit {
  protected userService = inject(UserService)
  protected alertService = inject(AlertService)
  protected accountService = inject(AccountService)
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected eventService = inject(EventService)
  private ngZone = inject(NgZone)
  protected featureService = inject(FeatureToggleService)
  private destroyRef = inject(DestroyRef)

  protected currentAccount = signal<IAccount | undefined>(undefined)
  protected users = signal<IUser[] | null | undefined>(null)
  protected totalItems = signal<number>(0)
  protected itemsPerPage = signal<number>(ITEMS_PER_PAGE)
  protected page = signal(1)
  protected sortColumn = signal('id')
  protected ascending = signal(true)
  protected itemCount = signal<string | null>(null)
  protected searchTerm = signal<string>('')
  protected submittedSearchTerm = signal<string>('')

  protected faTimesCircle = faTimesCircle
  protected faCheckCircle = faCheckCircle
  protected faTimes = faTimes
  protected faSearch = faSearch
  protected faPlus = faPlus
  protected faPencilAlt = faPencilAlt
  protected faSignInAlt = faSignInAlt
  protected faSortDown = faSortDown
  protected faSortUp = faSortUp
  protected readonly DEFAULT_ADMIN = 'admin@orcid.org'
  protected isLoading = signal(false)

  ngOnInit() {
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => {
      this.page.set(data['queryParams'] ? data['queryParams'].page : 1)
      this.ascending.set(data['queryParams'] ? data['queryParams'].page.sort.split(',')[1] === 'asc' : true)
      this.sortColumn.set(data['queryParams'] ? data['queryParams'].page.sort.split(',')[0] : 'id')
    })
    this.featureService.initFeatures().subscribe()
    this.accountService.getAccountData().subscribe((account) => {
      if (account) {
        this.currentAccount.set(account)
      }
    })
    this.loadAll()

    this.eventService.on(EventType.USER_LIST_MODIFIED)
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
    if (this.hasRoleAdmin()) {
      this.userService
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
    } else {
      this.userService
        .findByMemberId(this.accountService.getMemberId(), {
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
    this.ngZone.run(() => {
      this.router.navigate(['/users'], {
        queryParams: {
          page: this.page(),
          size: this.itemsPerPage(),
          sort: this.sortColumn() + ',' + (this.ascending() ? 'asc' : 'desc'),
          filter: this.submittedSearchTerm() ? this.submittedSearchTerm() : '',
        },
      })
      this.loadAll()
    })
  }

  clear() {
    this.page.set(0)
    this.ngZone.run(() => {
      this.router.navigate([
        '/users',
        {
          page: this.page(),
          sort: this.sortColumn() + ',' + (this.ascending() ? 'asc' : 'desc'),
          filter: this.submittedSearchTerm() ? this.submittedSearchTerm() : '',
        },
      ])
      this.loadAll()
    })
  }

  sort() {
    const result = [this.sortColumn() + ',' + (this.ascending() ? 'asc' : 'desc')]
    if (this.sortColumn() !== 'id') {
      result.push('id')
    }
    return result
  }

  sendActivate(msUser: IUser) {
    this.userService.sendActivate(msUser).subscribe((res) => {
      if (res) {
        this.alertService.broadcast(AlertType.TOAST, AlertMessage.SEND_ACTIVATION_SUCCESS)
      } else {
        this.alertService.broadcast(AlertType.TOAST, AlertMessage.SEND_ACTIVATION_FAILURE)
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
    return msUser.email === this.currentAccount()?.email
  }

  isDefaultAdmin(msUser: IUser) {
    return msUser.email === this.DEFAULT_ADMIN
  }

  isUserLoggedIn(msUser: IUser) {
    return msUser.email === this.currentAccount()?.email
  }

  hasRoleAdmin() {
    return this.accountService.hasAnyAuthority(['ROLE_ADMIN'])
  }

  isOrganizationOwner() {
    return this.accountService.isOrganizationOwner()
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

  protected paginate(data: Page<User>) {
    this.totalItems.set(data.page.totalElements)
    this.users.set(data.content)

    // 1. Handle the "0 items" edge case
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
}
