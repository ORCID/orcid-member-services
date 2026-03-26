export const environment = {
  production: true,
  issuerUrl: (window as any).__env?.issuerUrl || 'http://localhost:9000',
  redirectUri: window.location.origin + '/auth/callback',
  postLogoutRedirectUri: window.location.origin + '/en/login',
}
