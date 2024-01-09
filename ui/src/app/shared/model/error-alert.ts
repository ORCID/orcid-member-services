import { EventType } from 'src/app/app.constants'

export class ErrorAlert {
  constructor(
    public type: 'danger',
    public msg: string,
    public params: string,
    public toast: boolean,
    public scoped: boolean
  ) {}
}
