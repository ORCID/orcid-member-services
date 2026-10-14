import { Component, ChangeDetectionStrategy, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { faEnvelope, faPencilAlt } from '@fortawesome/free-solid-svg-icons'
import { CLIENT_UPDATED_STATE_KEY, Client, isSlClient } from './model/client'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { ActivatedRoute, Router, RouterLink, RouterOutlet } from '@angular/router'
import { NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { AlertMessage, AlertType } from '../app.constants'
import { isConsortiumMember } from '../member/model/member.model'
import { MemberService } from '../member/service/member.service'
import { AlertService } from '../shared/service/alert.service'
import { ClientUpdatedDialogComponent } from './client-updated-dialog.component'
import { ApiCredentialsService } from './service/api-credentials.service'

@Component({
  selector: 'app-api-credentials',
  templateUrl: './api-credentials.component.html',
  styleUrls: ['./api-credentials.component.scss'],
  imports: [FaIconComponent, RouterLink, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiCredentialsComponent implements OnInit {
  protected faEnvelope = faEnvelope
  protected faPencilAlt = faPencilAlt
  protected isSlClient = isSlClient

  private apiCredentialsService = inject(ApiCredentialsService)
  private memberService = inject(MemberService)
  private activatedRoute = inject(ActivatedRoute)
  private alertService = inject(AlertService)
  private modalService = inject(NgbModal)
  private destroyRef = inject(DestroyRef)

  // Read during construction because the navigation is only current until it completes.
  private updatedClientName = inject(Router).currentNavigation()?.extras.state?.[CLIENT_UPDATED_STATE_KEY] as
    | string
    | undefined

  protected productionCredentials = signal<Client[]>([])
  protected memberId = signal<string | null>(null)
  protected canApplyForAffiliationManager = signal(false)

  ngOnInit(): void {
    if (this.updatedClientName) {
      const ref = this.modalService.open(ClientUpdatedDialogComponent, {
        ariaLabelledBy: 'client-updated-title',
        centered: true,
        windowClass: 'client-updated-modal',
      })
      ref.componentInstance.clientName = this.updatedClientName
    }
    const memberId =
      this.activatedRoute.snapshot.paramMap.get('memberId') ??
      this.activatedRoute.parent?.snapshot.paramMap.get('memberId')
    if (!memberId) {
      this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_SEARCH_ERROR)
      return
    }
    this.memberId.set(memberId)
    this.memberService
      .find(memberId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (member) => this.canApplyForAffiliationManager.set(isConsortiumMember(member)),
        error: () => this.canApplyForAffiliationManager.set(false),
      })
    this.apiCredentialsService
      .getClientsForMember(memberId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => this.productionCredentials.set(result),
        error: () => this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_SEARCH_ERROR),
      })
  }
}
