import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { RouterModule } from '@angular/router'
import { routes } from './home.route'
import { HomeComponent } from './home.component'
import { MemberInfoComponent } from './member-info/member-info.component'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import { MemberInfoEditComponent } from './member-info/member-info-edit.component'
import { QuillModule } from 'ngx-quill'
import { ReactiveFormsModule } from '@angular/forms'
import { SharedModule } from '../shared/shared.module'

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    RouterModule.forChild(routes),
    FontAwesomeModule,
    QuillModule.forRoot(),
    ReactiveFormsModule,
  ],
  declarations: [HomeComponent, MemberInfoComponent, MemberInfoEditComponent],
})
export class HomeModule {}
