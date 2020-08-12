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
  addedToORCID?: Moment;
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
  orcidId?: string;
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
  updatedInORCID?: Moment;
  url?: string;
  lastModifiedBy?: string;
}

export class Assertion implements IAssertion {
  constructor(
    public id?: string,
    public addedToORCID?: Moment,
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
    public orcidId?: string,
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
    public updatedInORCID?: Moment,
    public url?: string,
    public lastModifiedBy?: string,
  ) {
    this.addedToORCID = this.addedToORCID || null;
    this.updated = this.updated || null;
    this.updatedInORCID = this.updatedInORCID || null;
  }
}
