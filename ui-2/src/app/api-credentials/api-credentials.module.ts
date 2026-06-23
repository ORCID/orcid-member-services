import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { ApiCredentialsComponent } from './api-credentials.component'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import { SharedModule } from '../shared/shared.module'
import { routes } from './api-credentials.route'
import { RouterModule } from '@angular/router'

@NgModule({
  imports: [RouterModule.forChild(routes), CommonModule, FontAwesomeModule, SharedModule, ApiCredentialsComponent],
})
export class ApiCredentialsModule {}
