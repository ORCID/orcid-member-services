import { ErrorHandler, NgModule } from '@angular/core'
import { BrowserModule } from '@angular/platform-browser'

import { CommonModule } from '@angular/common'
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'
import { FormsModule } from '@angular/forms'
import { AuthInterceptor, AuthModule, LogLevel } from 'angular-auth-oidc-client'
import { provideNgxWebstorage, withLocalStorage, withNgxWebstorageConfig, withSessionStorage } from 'ngx-webstorage'
import { environment } from '../environments/environment'
import { AccountModule } from './account/account.module'
import { AppRoutingModule } from './app-routing.module'
import { AppComponent } from './app.component'
import { ErrorComponent } from './error/error.component'
import { ErrorService } from './error/service/error.service'
import { HomeModule } from './home/home.module'
import { FooterComponent } from './layout/footer/footer.component'
import { NavbarComponent } from './layout/navbar/navbar.component'
import { AuthExpiredInterceptor } from './shared/interceptor/auth-expired.interceptor'
import { HeaderInterceptor } from './shared/interceptor/header.interceptor'
import { SharedModule } from './shared/shared.module'

@NgModule({
  declarations: [AppComponent, NavbarComponent, FooterComponent, ErrorComponent],
  bootstrap: [AppComponent], imports: [BrowserModule,
    HomeModule,
    AccountModule,
    AppRoutingModule,
    CommonModule,
    FormsModule,
    SharedModule.forRoot(),
    AuthModule.forRoot({
      config: {
        authority: environment.issuerUrl,
        redirectUrl: environment.redirectUri,
        postLogoutRedirectUri: environment.postLogoutRedirectUri,
        clientId: 'mp-ui-client',
        scope: 'openid MP',
        responseType: 'code',
        silentRenew: true,
        useRefreshToken: true,
        logLevel: LogLevel.Debug,
        secureRoutes: ['/userservice', '/memberservice', '/assertionservice'],
        autoUserInfo: false,
      },
    })],
  providers: [
      {
        provide: HTTP_INTERCEPTORS,
        useClass: AuthInterceptor,
        multi: true,
      },
      {
        provide: HTTP_INTERCEPTORS,
        useClass: HeaderInterceptor,
        multi: true,
      },
      {
        provide: HTTP_INTERCEPTORS,
        useClass: AuthExpiredInterceptor,
        multi: true,
      },
      {
        provide: ErrorHandler,
        useClass: ErrorService,
      },
      provideHttpClient(withInterceptorsFromDi()),
      provideNgxWebstorage(
        withNgxWebstorageConfig({ separator: ':', caseSensitive: true }),
        withLocalStorage(),
        withSessionStorage()
      ),
    ]
})
export class AppModule { }
