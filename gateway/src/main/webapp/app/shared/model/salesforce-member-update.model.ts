import { ISFAddress } from './salesforce-address.model';

export interface ISFMemberUpdate {
  name?: string;
  billingAddress?: ISFAddress;
  trademarkLicense?: string;
  publicName?: string;
  description?: string;
  website?: string;
  email?: string;
}

export class SFMemberUpdate implements ISFMemberUpdate {
  constructor(
    public name?: string,
    public billingAddress?: ISFAddress,
    public trademarkLicense?: string,
    public publicName?: string,
    public description?: string,
    public website?: string,
    public email?: string
  ) {}
}
