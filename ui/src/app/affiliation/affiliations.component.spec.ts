import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AffiliationsComponent } from './affiliations.component';

describe('AffiliationsComponent', () => {
  let component: AffiliationsComponent;
  let fixture: ComponentFixture<AffiliationsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AffiliationsComponent]
    });
    fixture = TestBed.createComponent(AffiliationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
