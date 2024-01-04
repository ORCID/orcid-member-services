export class AppError {
  constructor(
    public statusCode: number,
    public message: string
  ) {}
}
