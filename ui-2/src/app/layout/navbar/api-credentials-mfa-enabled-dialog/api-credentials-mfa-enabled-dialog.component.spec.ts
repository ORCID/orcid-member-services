import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { RouterTestingModule } from '@angular/router/testing'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { ApiCredentialsMfaEnabledDialogComponent } from './api-credentials-mfa-enabled-dialog.component'

describe('ApiCredentialsMfaEnabledDialogComponent', () => {
  let component: ApiCredentialsMfaEnabledDialogComponent
  let fixture: ComponentFixture<ApiCredentialsMfaEnabledDialogComponent>
  let activeModal: jasmine.SpyObj<NgbActiveModal>

  beforeEach(() => {
    const activeModalSpy = jasmine.createSpyObj('NgbActiveModal', ['dismiss'])

    TestBed.configureTestingModule({
      declarations: [ApiCredentialsMfaEnabledDialogComponent],
      imports: [RouterTestingModule],
      providers: [{ provide: NgbActiveModal, useValue: activeModalSpy }],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })

    fixture = TestBed.createComponent(ApiCredentialsMfaEnabledDialogComponent)
    component = fixture.componentInstance
    activeModal = TestBed.inject(NgbActiveModal) as jasmine.SpyObj<NgbActiveModal>
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should dismiss the modal when dismiss() is called', () => {
    component.dismiss()

    expect(activeModal.dismiss).toHaveBeenCalled()
  })

  it('should dismiss the modal when the close button is clicked', () => {
    const closeButton = fixture.nativeElement.querySelector('button.close')
    closeButton.click()

    expect(activeModal.dismiss).toHaveBeenCalled()
  })
})
