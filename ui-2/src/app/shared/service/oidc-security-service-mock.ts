import { of } from 'rxjs'

export class OidcSecurityServiceMock {
  checkAuth() {
    return of({
      isAuthenticated: true,
      userData: { name: 'Test User' },
      accessToken: 'fake-access-token',
      idToken: 'fake-id-token',
      configId: 'config-id',
    })
  }

  authorize() {
    // No-op for tests.
  }

  logoff() {
    return of({ action: 'logoff' })
  }

  getAccessToken() {
    return of('fake-access-token')
  }
}
