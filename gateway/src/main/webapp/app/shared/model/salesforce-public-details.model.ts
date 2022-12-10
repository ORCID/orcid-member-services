export interface ISFPublicDetails {
  name?: string;
  description?: string;
  website?: string;
  email?: string;
}

export class SFPublicDetails implements ISFPublicDetails {
  constructor(public name?: string, public description?: string, public website?: string, public email?: string) {}
}
