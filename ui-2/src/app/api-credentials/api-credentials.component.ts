import { Component, ChangeDetectionStrategy, OnInit, signal } from '@angular/core'
import { faEnvelope, faPencilAlt, faPlus } from '@fortawesome/free-solid-svg-icons'
import { ApiCredential } from './model/api-credential'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { RouterOutlet } from '@angular/router'

@Component({
  selector: 'app-api-credentials',
  templateUrl: './api-credentials.component.html',
  styleUrls: ['./api-credentials.component.scss'],
  imports: [FaIconComponent, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiCredentialsComponent implements OnInit {
  protected faEnvelope = faEnvelope
  protected faPencilAlt = faPencilAlt
  protected faPlus = faPlus

  protected productionCredentials = signal<ApiCredential[]>([])
  protected sandboxCredentials = signal<ApiCredential[]>([])

  ngOnInit(): void {
    // TODO: replace with real service call
    this.productionCredentials.set([
      {
        clientName: 'Example Production Credential',
        clientId: 'abc123',
        editable: true,
      },
      {
        clientName: 'Example Read-Only Production Credential',
        clientId: 'def456',
        editable: false,
      },
      {
        clientName: 'Example Write-Only Production Credential',
        clientId: 'deddsdf456',
        editable: false,
      },
    ])
    this.sandboxCredentials.set([])
  }
}
