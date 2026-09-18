import { ComponentFixture, TestBed } from '@angular/core/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { ResetClientSecretDialogComponent } from './reset-client-secret-dialog.component'

describe('ResetClientSecretDialogComponent', () => {
  let component: ResetClientSecretDialogComponent
  let fixture: ComponentFixture<ResetClientSecretDialogComponent>
  let activeModal: jasmine.SpyObj<NgbActiveModal>

  beforeEach(() => {
    const activeModalSpy = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss'])

    TestBed.configureTestingModule({
      imports: [ResetClientSecretDialogComponent],
      providers: [{ provide: NgbActiveModal, useValue: activeModalSpy }],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })

    activeModal = TestBed.inject(NgbActiveModal) as jasmine.SpyObj<NgbActiveModal>
    fixture = TestBed.createComponent(ResetClientSecretDialogComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should close the modal with "confirmed" on confirm', () => {
    component.confirm()
    expect(activeModal.close).toHaveBeenCalledWith('confirmed')
  })

  it('should dismiss the modal on cancel', () => {
    component.cancel()
    expect(activeModal.dismiss).toHaveBeenCalled()
  })
})
