import { ComponentFixture, TestBed } from '@angular/core/testing'

import { PasswordResetFinishComponent } from './password-reset-finish.component'
import { AppModule } from 'src/app/app.module'
import { PasswordService } from '../service/password.service'
import { of, throwError } from 'rxjs'

describe('PasswordResetFinishComponent', () => {
  let component: PasswordResetFinishComponent
  let fixture: ComponentFixture<PasswordResetFinishComponent>
  let service: PasswordService

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppModule],
      declarations: [PasswordResetFinishComponent],
    })
    fixture = TestBed.createComponent(PasswordResetFinishComponent)
    service = fixture.debugElement.injector.get(PasswordService)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('password should save successfully', () => {
    spyOn(service, 'savePassword').and.returnValue(of(true))
    component.finishReset()
    expect(component.success).toEqual('OK')
  })

  it('password save should fail', () => {
    spyOn(service, 'savePassword').and.returnValue(throwError(() => new Error('error')))
    component.finishReset()
    expect(component.error).toEqual('ERROR')
    expect(component.success).toBeFalsy()
  })
})
