/**
 * Fields supplied by the member when requesting Affiliation Manager credentials.
 * The member-service derives the requester from the authenticated session; the organisation
 * details are sent so the member can fill in any value that could not be looked up.
 */
export interface AffiliationManagerCredentialsApplication {
  orgName?: string
  consortiumLeadName?: string
  orgHomePage?: string
  notes?: string
}
