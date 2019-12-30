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

export interface IAffiliation {
  id?: string;
  email?: string;
  affiliationSection?: AffiliationSection;
  departmentName?: string;
  roleTitle?: string;
  startYear?: string;
  startMonth?: string;
  startDay?: string;
  endYear?: string;
  endMonth?: string;
  endDay?: string;
  orgName?: string;
  orgCountry?: string;
  orgCity?: string;
  orgRegion?: string;
  disambiguatedOrgId?: string;
  disambiguationSource?: string;
  externalId?: string;
  externalIdType?: string;
  externalIdUrl?: string;
  putCode?: string;
  created?: Moment;
  modified?: Moment;
  deletedFromORCID?: Moment;
  sent?: boolean;
  adminId?: string;
}

export class Affiliation implements IAffiliation {
  constructor(
    public id?: string,
    public email?: string,
    public affiliationSection?: AffiliationSection,
    public departmentName?: string,
    public roleTitle?: string,
    public startYear?: string,
    public startMonth?: string,
    public startDay?: string,
    public endYear?: string,
    public endMonth?: string,
    public endDay?: string,
    public orgName?: string,
    public orgCountry?: string,
    public orgCity?: string,
    public orgRegion?: string,
    public disambiguatedOrgId?: string,
    public disambiguationSource?: string,
    public externalId?: string,
    public externalIdType?: string,
    public externalIdUrl?: string,
    public putCode?: string,
    public created?: Moment,
    public modified?: Moment,
    public deletedFromORCID?: Moment,
    public sent?: boolean,
    public adminId?: string
  ) {
    this.sent = this.sent || false;
  }
}
