import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { RouterModule } from '@angular/router'
import { routes } from './home.route'
import { HomeComponent } from './home.component'
import { MemberInfoComponent } from './member-info/member-info.component'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'

@NgModule({
  imports: [CommonModule, RouterModule.forChild(routes), FontAwesomeModule],
  declarations: [HomeComponent, MemberInfoComponent],
})
export class HomeModule {}
