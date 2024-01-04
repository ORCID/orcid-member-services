import { EventType } from 'src/app/app.constants'

export class Error {
  constructor(
    public statusCode: number,
    public message: string
  ) {}
}
