import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { RouterModule } from '@angular/router'
import { memberRoutes } from './member.route'
import { SharedModule } from '../shared/shared.module'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { MembersComponent } from './members.component'
import { MemberUpdateComponent } from './member-update.component'
import { MemberDetailComponent } from './member-detail.component'

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    RouterModule.forChild(memberRoutes),
    FontAwesomeModule,
    FormsModule,
    ReactiveFormsModule,
  ],
  declarations: [MembersComponent, MemberUpdateComponent, MemberDetailComponent],
})
export class MemberModule {}
