export interface Client {
  clientName: string
  clientId: string
  editable: boolean
  homepageUrl?: string
  description?: string
  clientSecret?: string
  redirectUris?: string[]
}

export const CLIENT_DESCRIPTION_MAX_LENGTH = 300
