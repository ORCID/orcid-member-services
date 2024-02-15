import { ComponentFixture, TestBed } from '@angular/core/testing'

import { UserDeletePopupComponent } from './user-delete.component'
import { RouterTestingModule } from '@angular/router/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'

describe('UserDeleteComponent', () => {
  let component: UserDeletePopupComponent
  let fixture: ComponentFixture<UserDeletePopupComponent>

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [UserDeletePopupComponent],
      imports: [RouterTestingModule],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
    fixture = TestBed.createComponent(UserDeletePopupComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
