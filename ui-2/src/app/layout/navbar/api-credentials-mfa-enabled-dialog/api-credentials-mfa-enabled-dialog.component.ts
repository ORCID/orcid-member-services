import { Component } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { faTimesCircle } from '@fortawesome/free-solid-svg-icons'

@Component({
  selector: 'app-api-credentials-mfa-enabled-dialog',
  templateUrl: './api-credentials-mfa-enabled-dialog.component.html',
  styleUrls: ['./api-credentials-mfa-enabled-dialog.component.scss'],
})
export class ApiCredentialsMfaEnabledDialogComponent {
  faTimesCircle = faTimesCircle

  constructor(private activeModal: NgbActiveModal) {}

  dismiss() {
    this.activeModal.dismiss()
  }
}
