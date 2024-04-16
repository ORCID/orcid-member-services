import { NgModule } from '@angular/core'
import { CommonModule } from '@angular/common'
import { RouterModule } from '@angular/router'
import { routes } from './home.route'
import { HomeComponent } from './home.component';
import { GenericLandingComponent } from './generic-landing.component'

@NgModule({
  declarations: [HomeComponent, GenericLandingComponent],
  imports: [CommonModule, RouterModule.forChild(routes)],
})
export class HomeModule {}
