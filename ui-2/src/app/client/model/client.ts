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

export function isSlClient(client: Pick<Client, 'slClient'>): boolean {
  return client.slClient === true
}
