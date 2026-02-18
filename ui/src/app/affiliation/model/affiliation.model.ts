import { Moment } from 'moment'

export const enum AffiliationSection {
  EMPLOYMENT = 'EMPLOYMENT',
  EDUCATION = 'EDUCATION',
  QUALIFICATION = 'QUALIFICATION',
  INVITED_POSITION = 'INVITED_POSITION',
  DISTINCTION = 'DISTINCTION',
  MEMBERSHIP = 'MEMBERSHIP',
  SERVICE = 'SERVICE',
}

export interface IAffiliation {
  addedToORCID?: Moment | null
  affiliationSection?: AffiliationSection
  created?: Moment
  deletedFromORCID?: Moment
  departmentName?: string
  disambiguatedOrgId?: string
  disambiguationSource?: string
  email?: string
  endDay?: string
  endMonth?: string
  endYear?: string
  externalId?: string
  externalIdType?: string
  externalIdUrl?: string
  id?: string
  modified?: Moment
  orcidError?: string
  orcidId?: string
  orgCity?: string
  orgCountry?: string
  orgName?: string
  orgRegion?: string
  ownerId?: string
  putCode?: string
  roleTitle?: string
  salesforceId?: string
  startMonth?: string
  startDay?: string
  startYear?: string
  status?: string
  updated?: boolean | null
  updatedInORCID?: Moment | null
  url?: string
  lastModifiedBy?: string
  permissionLink?: string | null
  prettyStatus?: string
  notificationSent?: Moment
  invitationSent?: Moment
  notificationLastSent?: Moment
  invitationLastSent?: Moment
}

export class Affiliation implements IAffiliation {
  constructor(
    public id?: string,
    public addedToORCID?: Moment | null,
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
    public updated?: boolean | null,
    public updatedInORCID?: Moment | null,
    public url?: string,
    public lastModifiedBy?: string,
    public permissionLink?: string | null,
    public prettyStatus?: string,
    public notificationSent?: Moment,
    public notificationLastSent?: Moment,
    public invitationSent?: Moment,
    public invitationLastSent?: Moment
  ) {
    this.addedToORCID = this.addedToORCID || null
    this.updated = this.updated || null
    this.updatedInORCID = this.updatedInORCID || null
    this.permissionLink = this.permissionLink || null
  }
}
