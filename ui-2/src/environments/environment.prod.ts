export const environment = {
  production: true,
  issuerUrl: (window as any).__env?.issuerUrl || 'http://localhost:9030',
  redirectUri: window.location.origin + '/en/auth/callback',
  postLogoutRedirectUri: window.location.origin + '/en/login',
}
