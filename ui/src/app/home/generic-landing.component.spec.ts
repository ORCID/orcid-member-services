import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GenericLandingComponent } from './generic-landing.component';

describe('GenericLandingComponent', () => {
  let component: GenericLandingComponent;
  let fixture: ComponentFixture<GenericLandingComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [GenericLandingComponent]
    });
    fixture = TestBed.createComponent(GenericLandingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
