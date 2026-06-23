import { ComponentFixture, TestBed } from '@angular/core/testing'
import { EMPTY } from 'rxjs'

import { ErrorAlertComponent } from './error-alert.component'
import { ErrorService } from './service/error.service'

describe('ErrorAlertComponent', () => {
  let component: ErrorAlertComponent
  let fixture: ComponentFixture<ErrorAlertComponent>

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ErrorAlertComponent],
      providers: [{ provide: ErrorService, useValue: { on: () => EMPTY } }],
    })
    fixture = TestBed.createComponent(ErrorAlertComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
