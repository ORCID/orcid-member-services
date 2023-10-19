import { ISFAddress } from './salesforce-address.model';

export interface ISFMemberUpdate {
  orgName?: string;
  billingAddress?: ISFAddress;
  trademarkLicense?: string;
  publicName?: string;
  description?: string;
  website?: string;
  email?: string;
}

export class SFMemberUpdate implements ISFMemberUpdate {
  constructor(
    public orgName?: string,
    public billingAddress?: ISFAddress,
    public trademarkLicense?: string,
    public publicName?: string,
    public description?: string,
    public website?: string,
    public email?: string
  ) {}
}
