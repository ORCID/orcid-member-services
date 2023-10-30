import { EventType } from 'src/app/app.constants'
import { EventService } from './event.service'
import { Event } from '../model/event.model'

describe('EventService', () => {
  let eventService: EventService

  beforeEach(() => {
    eventService = new EventService()
  })

  it('should be created', () => {
    expect(eventService).toBeTruthy()
  })

  it('should broadcast events', () => {
    const event: Event = {
      type: EventType.LOG_IN_SUCCESS,
      payload: 'Login successful',
    }
    let receivedEvent: Event | undefined

    eventService.on(EventType.LOG_IN_SUCCESS).subscribe((e) => {
      receivedEvent = e
    })

    eventService.broadcast(event)

    expect(receivedEvent).toEqual(event)
  })

  it('should filter events by type', () => {
    const event1: Event = { type: EventType.LOG_IN_SUCCESS, payload: 'data 1' }
    const event2: Event = {
      type: EventType.AFFILIATION_CREATED,
      payload: 'data 2',
    }
    let receivedEvent: Event | undefined

    eventService.on(EventType.LOG_IN_SUCCESS).subscribe((e) => {
      receivedEvent = e
    })

    eventService.broadcast(event1)
    eventService.broadcast(event2)

    expect(receivedEvent).toEqual(event1)
  })
})
