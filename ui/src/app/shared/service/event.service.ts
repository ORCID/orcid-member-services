import { Injectable } from '@angular/core'
import { Observable, Subject, filter } from 'rxjs'
import { EventType } from 'src/app/app.constants'
import { Event } from '../model/event.model'

@Injectable({ providedIn: 'root' })
export class EventService {
  private events = new Subject<Event>()

  on(eventType: EventType): Observable<Event> {
    return this.events.pipe(filter((event) => event.type === eventType))
  }

  broadcast(event: Event): void {
    this.events.next(event)
  }
}
