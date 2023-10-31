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
import { environment } from 'src/environments/environment'

@NgModule({
  declarations: [AppComponent, NavbarComponent, HasAnyAuthorityDirective],
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
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {
  constructor() {
    environment.SERVER_API_URL = environment.SERVER_API_URL.replace('<DOMAIN>', window.location.origin)
  }
}
