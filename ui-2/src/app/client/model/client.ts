export interface Client {
  clientName: string
  clientId: string
  editable: boolean
  slClient?: boolean
  homepageUrl?: string
  description?: string
  clientSecret?: string
  redirectUris?: string[]
}

export const CLIENT_DESCRIPTION_MAX_LENGTH = 300

// Navigation state key the Edit client page uses to pass the updated client's name back to the list.
export const CLIENT_UPDATED_STATE_KEY = 'clientUpdated'

export function isSlClient(client: Pick<Client, 'slClient'>): boolean {
  return client.slClient === true
}
