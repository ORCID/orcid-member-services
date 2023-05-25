export interface ISFNewConsortiumMember {
  orgName: string;
  trademarkLicense: string;
  startMonth: string;
  startYear: string;
  requestedByName: string;
  requestedByEmail: string;
  consortium: string;
  orgEmailDomain?: string;
  street?: string;
  city?: string;
  state?: string;
  country?: string;
  postcode?: string;
  contactName?: string;
  contactJobTitle?: string;
  contactEmail?: string;
  contactPhone?: string;
}

export class SFNewConsortiumMember implements ISFNewConsortiumMember {
  constructor(
    public orgName: string,
    public trademarkLicense: string,
    public startMonth: string,
    public startYear: string,
    public requestedByName: string,
    public requestedByEmail: string,
    public consortium: string,
    public orgEmailDomain?: string,
    public street?: string,
    public city?: string,
    public state?: string,
    public country?: string,
    public postcode?: string,
    public contactName?: string,
    public contactJobTitle?: string,
    public contactEmail?: string,
    public contactPhone?: string
  ) {}
}
