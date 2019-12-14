export interface IDummy {
  id?: string;
  login?: string;
}

export class Dummy implements IDummy {
  constructor(public id?: string, public login?: string) {}
}
