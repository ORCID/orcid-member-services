import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { faBan, faRedo } from '@fortawesome/free-solid-svg-icons'

@Component({
  selector: 'app-reset-client-secret-dialog',
  templateUrl: './reset-client-secret-dialog.component.html',
  imports: [FaIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResetClientSecretDialogComponent {
  protected activeModal = inject(NgbActiveModal)

  // set by the opener via modalRef.componentInstance
  clientId: string | undefined
  currentSecret: string | undefined

  protected faBan = faBan
  protected faRedo = faRedo

  confirm() {
    this.activeModal.close('confirmed')
  }

  cancel() {
    this.activeModal.dismiss()
  }
}

