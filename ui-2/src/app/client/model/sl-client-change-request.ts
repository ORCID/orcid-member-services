/**
 * Fields supplied by the member when requesting changes to a Search & Link client.
 * The member-service derives the requester and organisation details from the
 * authenticated session before it sends the notification email.
 */
export interface SlClientChangeRequest {
  homepageUrl: string
  redirectUris: string[]
}
