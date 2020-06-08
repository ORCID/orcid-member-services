import { Moment } from 'moment';

export const enum AffiliationSection {
  EMPLOYMENT = 'EMPLOYMENT',
  EDUCATION = 'EDUCATION',
  QUALIFICATION = 'QUALIFICATION',
  INVITED_POSITION = 'INVITED_POSITION',
  DISTINCTION = 'DISTINCTION',
  MEMBERSHIP = 'MEMBERSHIP',
  SERVICE = 'SERVICE'
}

export interface IAssertion {
  addedToORCID?: boolean;
  affiliationSection?: AffiliationSection;
  created?: Moment;
  deletedFromORCID?: Moment;
  departmentName?: string;
  disambiguatedOrgId?: string;
  disambiguationSource?: string;
  email?: string;
  endDay?: string;
  endMonth?: string;
  endYear?: string;
  externalId?: string;
  externalIdType?: string;
  externalIdUrl?: string;
  id?: string;
  modified?: Moment;
  orcidError?: string;
  orgCity?: string;
  orgCountry?: string;
  orgName?: string;
  orgRegion?: string;
  ownerId?: string;
  putCode?: string;
  roleTitle?: string;
  salesforceId?: string;
  startMonth?: string;
  startDay?: string;
  startYear?: string;
  status?: string;
  updated?: boolean;
  updatedInORCID?: boolean;
  url?: string;
}

export class Assertion implements IAssertion {
  constructor(
    public id?: string,
    public addedToORCID?: boolean,
    public affiliationSection?: AffiliationSection,
    public created?: Moment,
    public deletedFromORCID?: Moment,
    public departmentName?: string,
    public disambiguatedOrgId?: string,
    public disambiguationSource?: string,
    public email?: string,
    public endDay?: string,
    public endMonth?: string,
    public endYear?: string,
    public externalId?: string,
    public externalIdType?: string,
    public externalIdUrl?: string,
    public modified?: Moment,
    public orcidError?: string,
    public orgCity?: string,
    public orgCountry?: string,
    public orgName?: string,
    public orgRegion?: string,
    public ownerId?: string,
    public putCode?: string,
    public roleTitle?: string,
    public salesforceId?: string,
    public startMonth?: string,
    public startDay?: string,
    public startYear?: string,
    public status?: string,
    public updated?: boolean,
    public updatedInORCID?: boolean,
    public url?: string
  ) {
    this.addedToORCID = this.addedToORCID || false;
    this.updated = this.updated || false;
    this.updatedInORCID = this.updatedInORCID || false;
  }
}
