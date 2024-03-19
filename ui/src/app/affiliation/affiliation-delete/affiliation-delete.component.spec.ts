import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AffiliationDeleteComponent } from './affiliation-delete.component';

describe('AffiliationDeleteComponent', () => {
  let component: AffiliationDeleteComponent;
  let fixture: ComponentFixture<AffiliationDeleteComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffiliationDeleteComponent]
    });
    fixture = TestBed.createComponent(AffiliationDeleteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
