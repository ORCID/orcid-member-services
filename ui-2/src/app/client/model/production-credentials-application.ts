/**
 * Fields supplied by the member when requesting production API credentials.
 * The member-service derives the requester and organisation details from the
 * authenticated session before it sends the notification email.
 */
export interface ProductionCredentialsApplication {
  systemIntegrationType: string
  authenticateIdsAnswer: 'YES' | 'NO'
  integrationDisplayName: string
  integrationHomepageUrl: string
  integrationDescription: string
  redirectUris: string[]
  notes?: string
}
