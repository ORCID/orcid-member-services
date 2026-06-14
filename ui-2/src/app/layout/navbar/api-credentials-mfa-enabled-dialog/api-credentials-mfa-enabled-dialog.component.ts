import { Component, inject } from '@angular/core'
import { faTimesCircle } from '@fortawesome/free-solid-svg-icons'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'

@Component({
  selector: 'app-api-credentials-mfa-enabled-dialog',
  templateUrl: './api-credentials-mfa-enabled-dialog.component.html',
  styleUrls: ['./api-credentials-mfa-enabled-dialog.component.scss'],
  standalone: false,
})
export class ApiCredentialsMfaEnabledDialogComponent {
  faTimesCircle = faTimesCircle
  private activeModal = inject(NgbActiveModal)

  dismiss() {
    this.activeModal.dismiss()
  }
}
