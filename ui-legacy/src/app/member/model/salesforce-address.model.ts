export interface ISFAddress {
  city?: string
  country?: string
  countryCode?: string
  postalCode?: string
  state?: string
  stateCode?: string
  street?: string
}

export class SFAddress implements ISFAddress {
  constructor(
    public city?: string,
    public country?: string,
    public countryCode?: string,
    public postalCode?: string,
    public state?: string,
    public stateCode?: string,
    public street?: string
  ) {}
}
