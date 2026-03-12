import { Component, OnInit } from '@angular/core'
import { Affiliation, IAffiliation } from './model/affiliation.model'
import { ORCID_BASE_URL } from '../app.constants'
import { ActivatedRoute } from '@angular/router'
import { UserService } from '../user/service/user.service'
import { faPencilAlt, faArrowLeft } from '@fortawesome/free-solid-svg-icons'
import { DateUtilService } from '../shared/service/date-util.service'

@Component({
  selector: 'app-affiliation-detail',
  templateUrl: './affiliation-detail.component.html',
})
export class AffiliationDetailComponent implements OnInit {
  affiliation: IAffiliation = new Affiliation()
  orcidBaseUrl: string = ORCID_BASE_URL
  ownerId: string | undefined
  faPencilAlt = faPencilAlt
  faArrowLeft = faArrowLeft
  startDate: string | undefined
  endDate: string | undefined

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected userService: UserService,
    protected dateUtilService: DateUtilService
  ) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ affiliation }) => {
      this.affiliation = affiliation
      this.startDate = this.dateUtilService.formatDate({
        year: affiliation.startYear,
        month: affiliation.startMonth,
        day: affiliation.startDay,
      })
      this.endDate = this.dateUtilService.formatDate({
        year: affiliation.endYear,
        month: affiliation.endMonth,
        day: affiliation.endDay,
      })
      this.userService.find(this.affiliation!.ownerId!).subscribe((user) => {
        this.ownerId = user.email
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
