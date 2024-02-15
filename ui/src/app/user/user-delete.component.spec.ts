import { ComponentFixture, TestBed } from '@angular/core/testing'

import { UserDeletePopupComponent } from './user-delete.component'

describe('UserDeleteComponent', () => {
  let component: UserDeletePopupComponent
  let fixture: ComponentFixture<UserDeletePopupComponent>

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [UserDeletePopupComponent],
    })
    fixture = TestBed.createComponent(UserDeletePopupComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
