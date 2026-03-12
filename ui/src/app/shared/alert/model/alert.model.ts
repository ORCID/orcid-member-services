import { AlertType } from 'src/app/app.constants'

export class AppAlert {
  constructor(
    public type: AlertType,
    public msg: string | undefined
  ) {}
}
