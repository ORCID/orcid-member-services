import { TestBed } from '@angular/core/testing'
import { RouterTestingModule } from '@angular/router/testing'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { AppComponent } from './app.component'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { OidcSecurityService } from 'angular-auth-oidc-client'
import { OidcSecurityServiceMock } from './shared/service/oidc-security-service-mock'
import { of } from 'rxjs'
import { AccountService, StateStorageService } from './account'
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('AppComponent', () => {
  beforeEach(async () => {
    const stateStorageServiceSpy = jasmine.createSpyObj('StateStorageService', ['getUrl', 'storeUrl'])
    const accountServiceMock = {
      getAccountData: () => of(null), // return an observable
      isAuthenticated: () => false,
    }

    await TestBed.configureTestingModule({
    declarations: [AppComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [RouterTestingModule],
    providers: [
        { provide: AccountService, useValue: accountServiceMock },
        { provide: StateStorageService, useValue: stateStorageServiceSpy },
        { provide: OidcSecurityService, useClass: OidcSecurityServiceMock },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
    ]
}).compileComponents()
  })

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent)
    const app = fixture.componentInstance
    expect(app).toBeTruthy()
  })
})
