import { ComponentFixture, TestBed } from '@angular/core/testing'

import { ErrorAlertComponent } from './error-alert.component'
import { AppModule } from 'src/app/app.module'

describe('ErrorAlertComponent', () => {
  let component: ErrorAlertComponent
  let fixture: ComponentFixture<ErrorAlertComponent>

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppModule],
      declarations: [ErrorAlertComponent],
    })
    fixture = TestBed.createComponent(ErrorAlertComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
