import { Component, ChangeDetectionStrategy, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { faEnvelope, faPencilAlt, faPlus } from '@fortawesome/free-solid-svg-icons'
import { Client, isSlClient } from './model/client'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { ActivatedRoute, RouterLink, RouterOutlet } from '@angular/router'
import { AlertMessage, AlertType } from '../app.constants'
import { isConsortiumMember } from '../member/model/member.model'
import { MemberService } from '../member/service/member.service'
import { AlertService } from '../shared/service/alert.service'
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
  protected faPlus = faPlus
  protected isSlClient = isSlClient

  private apiCredentialsService = inject(ApiCredentialsService)
  private memberService = inject(MemberService)
  private activatedRoute = inject(ActivatedRoute)
  private alertService = inject(AlertService)
  private destroyRef = inject(DestroyRef)

  protected productionCredentials = signal<Client[]>([])
  protected sandboxCredentials = signal<Client[]>([])
  protected memberId = signal<string | null>(null)
  protected canApplyForAffiliationManager = signal(false)

  ngOnInit(): void {
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
