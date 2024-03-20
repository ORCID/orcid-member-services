import { ComponentFixture, TestBed } from '@angular/core/testing'

import { AffiliationDeletePopupComponent } from './affiliation-delete.component'

describe('AffiliationDeleteComponent', () => {
  let component: AffiliationDeletePopupComponent
  let fixture: ComponentFixture<AffiliationDeletePopupComponent>

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffiliationDeletePopupComponent],
    })
    fixture = TestBed.createComponent(AffiliationDeletePopupComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
