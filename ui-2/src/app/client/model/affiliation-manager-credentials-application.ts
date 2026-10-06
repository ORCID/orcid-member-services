/**
 * Fields supplied by the member when requesting Affiliation Manager credentials.
 * The member-service derives the requester, organisation, website and consortium lead
 * details from the authenticated session before it sends the notification email.
 */
export interface AffiliationManagerCredentialsApplication {
  notes?: string
}
