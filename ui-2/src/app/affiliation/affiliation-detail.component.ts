import { Component, ChangeDetectionStrategy, DestroyRef, OnInit, signal, inject } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { Affiliation, IAffiliation } from './model/affiliation.model'
import { ORCID_BASE_URL } from '../app.constants'
import { ActivatedRoute, RouterLink } from '@angular/router'
import { UserService } from '../user/service/user.service'
import { faPencilAlt, faArrowLeft } from '@fortawesome/free-solid-svg-icons'
import { DateUtilService } from '../shared/service/date-util.service'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { ClipboardModule } from 'ngx-clipboard'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { DatePipe } from '@angular/common'
import { LocalizePipe } from '../shared/pipe/localize'

@Component({
  selector: 'app-affiliation-detail',
  templateUrl: './affiliation-detail.component.html',
  imports: [ErrorAlertComponent, ClipboardModule, FaIconComponent, RouterLink, DatePipe, LocalizePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AffiliationDetailComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute)
  protected userService = inject(UserService)
  protected dateUtilService = inject(DateUtilService)
  private destroyRef = inject(DestroyRef)

  protected affiliation = signal<IAffiliation>(new Affiliation())
  protected readonly orcidBaseUrl = ORCID_BASE_URL
  protected ownerId = signal<string | undefined>(undefined)
  protected faPencilAlt = faPencilAlt
  protected faArrowLeft = faArrowLeft
  protected startDate = signal<string | undefined>(undefined)
  protected endDate = signal<string | undefined>(undefined)

  ngOnInit() {
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ affiliation }) => {
      this.affiliation.set(affiliation)
      this.startDate.set(this.dateUtilService.formatDate({
        year: affiliation.startYear,
        month: affiliation.startMonth,
        day: affiliation.startDay,
      }))
      this.endDate.set(this.dateUtilService.formatDate({
        year: affiliation.endYear,
        month: affiliation.endMonth,
        day: affiliation.endDay,
      }))
      this.userService.find(affiliation.ownerId!).subscribe((user) => {
        this.ownerId.set(user.email)
      })
    })
  }

  previousState() {
    window.history.back()
  }

  successMessage() {
    alert($localize`:@@gatewayApp.assertionServiceAssertion.copySuccess.string:Copied to clipboard`)
  }
}
