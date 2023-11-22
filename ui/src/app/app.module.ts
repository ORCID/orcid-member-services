import { NgModule } from '@angular/core'
import { BrowserModule } from '@angular/platform-browser'

import { AppRoutingModule } from './app-routing.module'
import { AppComponent } from './app.component'
import { HttpClientModule } from '@angular/common/http'
import { AccountModule } from './account/account.module'
import { NgxWebstorageModule } from 'ngx-webstorage'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import { NavbarComponent } from './layout/navbar/navbar.component'
import { CommonModule } from '@angular/common'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap'
import { HasAnyAuthorityDirective } from './shared/directive/has-any-authority.directive'
import { HomeModule } from './home/home.module'
import { FooterComponent } from './layout/footer/footer.component'
import { SharedModule } from './shared/shared.module'

@NgModule({
  declarations: [AppComponent, NavbarComponent, HasAnyAuthorityDirective, FooterComponent],
  imports: [
    FontAwesomeModule,
    AccountModule,
    HomeModule,
    BrowserModule,
    HttpClientModule,
    AppRoutingModule,
    NgxWebstorageModule.forRoot(),
    CommonModule,
    NgbModule,
    SharedModule.forRoot(),
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
