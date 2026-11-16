import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { faCheckCircle } from '@fortawesome/free-solid-svg-icons'

@Component({
  selector: 'app-client-updated-dialog',
  templateUrl: './client-updated-dialog.component.html',
  styleUrls: ['./client-updated-dialog.component.scss'],
  imports: [FaIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientUpdatedDialogComponent {
  protected activeModal = inject(NgbActiveModal)

  // set by the opener via modalRef.componentInstance
  clientName = ''

  protected faCheckCircle = faCheckCircle
}
