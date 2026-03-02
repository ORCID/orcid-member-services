export class AppError {
  constructor(
    public statusCode: number,
    public message: string
  ) {}
}

export class ErrorAlert {
  constructor(
    public type: 'danger',
    public msg: string,
    public toast: boolean
  ) {}
}