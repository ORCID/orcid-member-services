import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { RouterModule } from '@angular/router'
import { routes } from './home.route'
import { HomeComponent } from './home.component'
import { GenericLandingComponent } from './generic-landing.component'

@NgModule({
  imports: [CommonModule, RouterModule.forChild(routes)],
  declarations: [HomeComponent, GenericLandingComponent],
})
export class HomeModule {}
