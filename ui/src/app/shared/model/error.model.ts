export class AppError {
  constructor(
    public statusCode: number,
    public message: string,
    public i18nKey: string | null
  ) {}
}
