import { Component, ChangeDetectionStrategy, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { faEnvelope, faPencilAlt, faPlus } from '@fortawesome/free-solid-svg-icons'
import { Client } from './model/client'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { ActivatedRoute, RouterLink, RouterOutlet } from '@angular/router'
import { AlertMessage, AlertType } from '../app.constants'
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

  private apiCredentialsService = inject(ApiCredentialsService)
  private activatedRoute = inject(ActivatedRoute)
  private alertService = inject(AlertService)
  private destroyRef = inject(DestroyRef)

  protected productionCredentials = signal<Client[]>([])
  protected sandboxCredentials = signal<Client[]>([])
  protected memberId = signal<string | null>(null)

  ngOnInit(): void {
    const memberId =
      this.activatedRoute.snapshot.paramMap.get('memberId') ?? this.activatedRoute.parent?.snapshot.paramMap.get('memberId')
    if (!memberId) {
      this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_SEARCH_ERROR)
      return
    }
    this.memberId.set(memberId)
    this.apiCredentialsService
      .getClientsForMember(memberId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => this.productionCredentials.set(result),
        error: () => this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_SEARCH_ERROR),
      })
  }
}

