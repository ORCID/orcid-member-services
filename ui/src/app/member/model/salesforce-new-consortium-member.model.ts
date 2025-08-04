export interface ISFNewConsortiumMember {
  orgName: string
  trademarkLicense: string
  startMonth: string
  startYear: string
  emailDomain?: string
  street?: string
  city?: string
  state?: string
  country?: string
  postcode?: string
  contactGivenName?: string
  contactFamilyName?: string
  contactJobTitle?: string
  contactEmail?: string
  contactPhone?: string
  organizationTier?: string
  integrationPlans?: string
}

export class SFNewConsortiumMember implements ISFNewConsortiumMember {
  constructor(
    public orgName: string,
    public trademarkLicense: string,
    public startMonth: string,
    public startYear: string,
    public emailDomain?: string,
    public street?: string,
    public city?: string,
    public state?: string,
    public country?: string,
    public postcode?: string,
    public contactGivenName?: string,
    public contactFamilyName?: string,
    public contactJobTitle?: string,
    public contactEmail?: string,
    public contactPhone?: string
  ) {}
}

export interface Option<T extends string> {
  value: T;
  description: string;
}

export type TrademarkLicenseValue = 'Yes' | 'No';
export type OrganizationTierValue = 'Small' | 'Standard' | 'Large';

export type TrademarkLicenseOption = Option<TrademarkLicenseValue>;
export type OrganizationTierOption = Option<OrganizationTierValue>;
