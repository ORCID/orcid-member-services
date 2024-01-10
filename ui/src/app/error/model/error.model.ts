export class AppError {
  constructor(
    public statusCode: number,
    public message: string,
    public i18nKey: string | null
  ) {}
}

export class ErrorAlert {
  constructor(
    public type: 'danger',
    public msg: string,
    public toast: boolean
  ) {}
}