import { Component, OnInit, inject } from '@angular/core'
import { IMember } from './model/member.model'
import { faArrowLeft, faCheckCircle, faPencilAlt, faTimesCircle } from '@fortawesome/free-solid-svg-icons'
import { ActivatedRoute } from '@angular/router'

@Component({
  selector: 'app-member-detail',
  templateUrl: './member-detail.component.html',
  styleUrls: ['./member-detail.component.scss'],
  standalone: false,
})
export class MemberDetailComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute)

  member: IMember | undefined
  faTimesCircle = faTimesCircle
  faCheckCircle = faCheckCircle
  faArrowLeft = faArrowLeft
  faPencilAlt = faPencilAlt

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ member }) => {
      this.member = member
      if (!this.member!.type) {
        this.member!.type = 'unset'
      }
      if (!this.member!.status) {
        this.member!.status = 'unset'
      }
    })
  }

  previousState() {
    window.history.back()
  }
}
