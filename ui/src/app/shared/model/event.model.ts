import { EventType } from 'src/app/app.constants'

export class Event {
  constructor(
    public type: EventType,
    public payload: String
  ) {}
}
