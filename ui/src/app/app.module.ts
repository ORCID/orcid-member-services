import { ErrorHandler, NgModule } from '@angular/core'
import { BrowserModule } from '@angular/platform-browser'

import { AppRoutingModule } from './app-routing.module'
import { AppComponent } from './app.component'
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http'
import { AccountModule } from './account/account.module'
import { NgxWebstorageModule } from 'ngx-webstorage'
import { NavbarComponent } from './layout/navbar/navbar.component'
import { CommonModule } from '@angular/common'
import { HomeModule } from './home/home.module'
import { FooterComponent } from './layout/footer/footer.component'
import { SharedModule } from './shared/shared.module'
import { HeaderInterceptor } from './shared/interceptor/header.interceptor'
import { ErrorService } from './error/service/error.service'
import { ErrorComponent } from './error/error.component'
import { FormsModule } from '@angular/forms'
import { UserModule } from './user/user.module'
import { AffiliationModule } from './affiliation/affiliation.module'
import { MembersComponent } from './member/members.component'
import { MemberModule } from './member/member.module'

@NgModule({
  declarations: [AppComponent, NavbarComponent, FooterComponent, ErrorComponent],
  imports: [
    BrowserModule,
    UserModule,
    AffiliationModule,
    MemberModule,
    AccountModule,
    HomeModule,
    HttpClientModule,
    AppRoutingModule,
    NgxWebstorageModule.forRoot(),
    CommonModule,
    FormsModule,
    SharedModule.forRoot(),
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: HeaderInterceptor,
      multi: true,
    },
    {
      provide: ErrorHandler,
      useClass: ErrorService,
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
