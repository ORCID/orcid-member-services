/// <reference types="jasmine" />

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { RouterModule } from '@angular/router'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { ApiCredentialsMfaEnabledDialogComponent } from './api-credentials-mfa-enabled-dialog.component'

type ApiCredentialsMfaEnabledDialogInternals = {
  dismiss: () => void
}
const internals = (component: ApiCredentialsMfaEnabledDialogComponent): ApiCredentialsMfaEnabledDialogInternals =>
  component as unknown as ApiCredentialsMfaEnabledDialogInternals

describe('ApiCredentialsMfaEnabledDialogComponent', () => {
  let component: ApiCredentialsMfaEnabledDialogComponent
  let fixture: ComponentFixture<ApiCredentialsMfaEnabledDialogComponent>
  let activeModal: jasmine.SpyObj<NgbActiveModal>

  beforeEach(() => {
    const activeModalSpy = jasmine.createSpyObj('NgbActiveModal', ['dismiss'])

    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), ApiCredentialsMfaEnabledDialogComponent],
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
    internals(component).dismiss()

    expect(activeModal.dismiss).toHaveBeenCalled()
  })

  it('should dismiss the modal when the close button is clicked', () => {
    const closeButton = fixture.nativeElement.querySelector('button.close')
    closeButton.click()

    expect(activeModal.dismiss).toHaveBeenCalled()
  })
})
