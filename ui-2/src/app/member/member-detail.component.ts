import { Component, ChangeDetectionStrategy, DestroyRef, OnInit, signal, inject } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { IMember } from './model/member.model'
import { faArrowLeft, faCheckCircle, faPencilAlt, faTimesCircle } from '@fortawesome/free-solid-svg-icons'
import { ActivatedRoute, RouterLink } from '@angular/router'
import { AlertComponent } from '../shared/alert/alert-toast.component'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'

@Component({
  selector: 'app-member-detail',
  templateUrl: './member-detail.component.html',
  styleUrls: ['./member-detail.component.scss'],
  imports: [AlertComponent, FaIconComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MemberDetailComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute)
  private destroyRef = inject(DestroyRef)
  protected member = signal<IMember | undefined>(undefined)
  protected faTimesCircle = faTimesCircle
  protected faCheckCircle = faCheckCircle
  protected faArrowLeft = faArrowLeft
  protected faPencilAlt = faPencilAlt

  ngOnInit() {
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ member }) => {
      if (!member.type) {
        member.type = 'unset'
      }
      if (!member.status) {
        member.status = 'unset'
      }
      this.member.set(member)
    })
  }

  previousState() {
    window.history.back()
  }
}
