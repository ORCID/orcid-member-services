import { ComponentFixture, TestBed } from '@angular/core/testing'

import { AffiliationDeleteDialogComponent, AffiliationDeletePopupComponent } from './affiliation-delete.component'
import { RouterTestingModule } from '@angular/router/testing'
import { AffiliationService } from './service/affiliation.service'
import { EventService } from '../shared/service/event.service'
import { EventType } from '../app.constants'
import { Event } from '../shared/model/event.model'
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { HttpResponse } from '@angular/common/http'
import { of } from 'rxjs'

describe('AffiliationDeleteComponent', () => {
  let component: AffiliationDeleteDialogComponent
  let fixture: ComponentFixture<AffiliationDeleteDialogComponent>
  let affiliationService: jasmine.SpyObj<AffiliationService>
  let eventService: jasmine.SpyObj<EventService>

  beforeEach(() => {
    const affiliationServiceSpy = jasmine.createSpyObj('AffiliationService', ['delete'])
    const eventServiceSpy = jasmine.createSpyObj('EventService', ['broadcast'])

    TestBed.configureTestingModule({
      declarations: [AffiliationDeleteDialogComponent],
      imports: [RouterTestingModule],
      providers: [
        NgbModal,
        NgbActiveModal,
        { provide: AffiliationService, useValue: affiliationServiceSpy },
        { provide: EventService, useValue: eventServiceSpy },
      ],
    })
    affiliationService = TestBed.inject(AffiliationService) as jasmine.SpyObj<AffiliationService>
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>
    fixture = TestBed.createComponent(AffiliationDeleteDialogComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should confirm the affiliation being deleted', () => {
    affiliationService.delete.and.returnValue(of(true))
    component.confirmDelete('id')

    expect(eventService.broadcast).toHaveBeenCalledWith(
      new Event(EventType.AFFILIATION_LIST_MODIFICATION, 'Deleted an affiliation')
    )
  })

  it('should fail to delete the affiliation', () => {
    affiliationService.delete.and.returnValue(of(false))
    component.confirmDelete('id')

    expect(eventService.broadcast).toHaveBeenCalledWith(
      new Event(EventType.AFFILIATION_LIST_MODIFICATION, 'Failed to delete an affiliation')
    )
  })

  it('should not call the assertion service without an id', () => {
    affiliationService.delete.and.returnValue(of(false))
    component.confirmDelete('')

    expect(affiliationService.delete).toHaveBeenCalledTimes(0)
  })
})
