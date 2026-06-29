import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { IUser } from './model/user.model'
import { faCheckCircle, faTimesCircle, faPencilAlt, faArrowLeft } from '@fortawesome/free-solid-svg-icons'
import { ActivatedRoute, RouterLink } from '@angular/router'
import { UserService } from './service/user.service'
import { AlertService } from '../shared/service/alert.service'
import { MemberService } from '../member/service/member.service'
import { AlertMessage, AlertType } from '../app.constants'
import { ErrorAlertComponent } from '../error/error-alert.component'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { DatePipe } from '@angular/common'

@Component({
  selector: 'app-user-detail',
  templateUrl: './user-detail.component.html',
  imports: [ErrorAlertComponent, FaIconComponent, RouterLink, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserDetailComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute)
  private destroyRef = inject(DestroyRef)
  protected userService = inject(UserService)
  protected alertService = inject(AlertService)
  protected memberService = inject(MemberService)

  protected user = signal<IUser | null>(null)
  protected faTimesCircle = faTimesCircle
  protected faCheckCircle = faCheckCircle
  protected faPencilAlt = faPencilAlt
  protected faArrowLeft = faArrowLeft

  private DEFAULT_ADMIN = 'admin'
  protected superAdmin = signal<boolean | undefined>(false)

  ngOnInit() {
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ user }) => {
      this.user.set(user)
    })
  }

  sendActivate() {
    const user = this.user()
    if (user) {
      this.userService.sendActivate(user).subscribe(() => {
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
