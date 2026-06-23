import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { faTimesCircle } from '@fortawesome/free-solid-svg-icons'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { RouterLink } from '@angular/router'

@Component({
  selector: 'app-api-credentials-mfa-enabled-dialog',
  templateUrl: './api-credentials-mfa-enabled-dialog.component.html',
  styleUrls: ['./api-credentials-mfa-enabled-dialog.component.scss'],
  imports: [FaIconComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiCredentialsMfaEnabledDialogComponent {
  protected readonly faTimesCircle = faTimesCircle
  private readonly activeModal = inject(NgbActiveModal)
  private readonly dismissed = signal(false)

  protected dismiss() {
    this.dismissed.set(true)
    this.activeModal.dismiss()
  }
}
