import { Component, OnInit } from '@angular/core'
import { faEnvelope, faPencilAlt, faPlus } from '@fortawesome/free-solid-svg-icons'
import { ApiCredential } from './model/api-credential'

@Component({
  selector: 'app-api-credentials',
  templateUrl: './api-credentials.component.html',
  styleUrls: ['./api-credentials.component.scss'],
})
export class ApiCredentialsComponent implements OnInit {
  faEnvelope = faEnvelope
  faPencilAlt = faPencilAlt
  faPlus = faPlus

  productionCredentials: ApiCredential[] = []
  sandboxCredentials: ApiCredential[] = []

  ngOnInit(): void {
    // TODO: replace with real service call
    this.productionCredentials = [{
      clientName: 'Example Production Credential',
      clientId: 'abc123',
      editable: true,
    }, {
      clientName: 'Example Read-Only Production Credential',
      clientId: 'def456',
      editable: false,
    }, {
      clientName: 'Example Write-Only Production Credential',
      clientId: 'deddsdf456',
      editable: false,
    }]
    this.sandboxCredentials = []
  }
}
