import { Component, inject } from '@angular/core'
import { IUser } from './model/user.model'
import { faCheckCircle, faTimesCircle, faPencilAlt, faArrowLeft } from '@fortawesome/free-solid-svg-icons'
import { ActivatedRoute } from '@angular/router'
import { map, switchMap, tap } from 'rxjs'
import { UserService } from './service/user.service'
import { AlertService } from '../shared/service/alert.service'
import { MemberService } from '../member/service/member.service'
import { AlertMessage, AlertType } from '../app.constants'

@Component({
  selector: 'app-user-detail',
  templateUrl: './user-detail.component.html',
  standalone: false,
})
export class UserDetailComponent {
  protected activatedRoute = inject(ActivatedRoute)
  protected userService = inject(UserService)
  protected alertService = inject(AlertService)
  protected memberService = inject(MemberService)

  user: IUser | null = null
  faTimesCircle = faTimesCircle
  faCheckCircle = faCheckCircle
  faPencilAlt = faPencilAlt
  faArrowLeft = faArrowLeft

  DEFAULT_ADMIN = 'admin'
  superAdmin: boolean | undefined = false

  ngOnInit() {
    this.activatedRoute.data.subscribe(({ user }) => {
      this.user = user
    })
  }

  sendActivate() {
    if (this.user) {
      this.userService.sendActivate(this.user).subscribe((res) => {
        this.alertService.broadcast(AlertType.TOAST, AlertMessage.SEND_ACTIVATION_SUCCESS)
        this.previousState()
      })
    }
  }

  isDefaultAdmin(user: IUser) {
    if (user.email === this.DEFAULT_ADMIN) {
      return true
    }
    return false
  }

  previousState() {
    window.history.back()
  }
}
