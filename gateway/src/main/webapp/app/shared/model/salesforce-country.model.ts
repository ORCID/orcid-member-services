import { ISFState } from './salesforce-country.model copy';

export interface ISFCountry {
  code: string;
  name: string;
  states?: ISFState[];
}

export class SFCountry implements ISFCountry {
  constructor(public code: string, public name: string, public states?: ISFState[]) {}
}
