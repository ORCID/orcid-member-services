import { ComponentFixture, TestBed } from '@angular/core/testing'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { ClientUpdatedDialogComponent } from './client-updated-dialog.component'

describe('ClientUpdatedDialogComponent', () => {
  let fixture: ComponentFixture<ClientUpdatedDialogComponent>
  let activeModal: jasmine.SpyObj<NgbActiveModal>

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ClientUpdatedDialogComponent],
      providers: [{ provide: NgbActiveModal, useValue: jasmine.createSpyObj('NgbActiveModal', ['dismiss']) }],
    })
    activeModal = TestBed.inject(NgbActiveModal) as jasmine.SpyObj<NgbActiveModal>
    fixture = TestBed.createComponent(ClientUpdatedDialogComponent)
    fixture.componentInstance.clientName = 'BieJournals (test system)'
    fixture.detectChanges()
  })

  it('should show the title and the updated client name in bold', () => {
    const element: HTMLElement = fixture.nativeElement
    expect(element.querySelector('.modal-title')?.textContent).toContain('Client updated')
    expect(element.querySelector('.modal-body strong')?.textContent).toBe('BieJournals (test system)')
    expect(element.querySelector('.modal-body')?.textContent).toContain('has been successfully updated.')
  })

  it('should close when the close button is clicked', () => {
    fixture.nativeElement.querySelector('[data-cy="closeClientUpdated"]').click()

    expect(activeModal.dismiss).toHaveBeenCalled()
  })
})
