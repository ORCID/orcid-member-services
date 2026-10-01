import { TestBed } from '@angular/core/testing'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { AppComponent } from './app.component'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { OidcSecurityService, PublicEventsService } from 'angular-auth-oidc-client'
import { OidcSecurityServiceMock } from './shared/service/oidc-security-service-mock'
import { EMPTY, of } from 'rxjs'
import { AccountService, StateStorageService } from './account'
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'
import { provideRouter } from '@angular/router'

describe('AppComponent', () => {
  beforeEach(async () => {
    const stateStorageServiceSpy = jasmine.createSpyObj('StateStorageService', ['getUrl', 'storeUrl'])
    const accountServiceMock = {
      getAccountData: () => of(null), // return an observable
      isAuthenticated: () => false,
    }

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        provideRouter([]),
        { provide: AccountService, useValue: accountServiceMock },
        { provide: StateStorageService, useValue: stateStorageServiceSpy },
        { provide: OidcSecurityService, useClass: OidcSecurityServiceMock },
        { provide: PublicEventsService, useValue: { registerForEvents: () => EMPTY } },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    }).compileComponents()
  })

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent)
    const app = fixture.componentInstance
    expect(app).toBeDefined()
  })
})
