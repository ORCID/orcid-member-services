export interface ISFState {
  code: string
  name: string
}

export class SFState implements ISFState {
  constructor(
    public code: string,
    public name: string
  ) {}
}
