import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PasswordResetFinishComponent } from './password-reset-finish.component';

describe('PasswordResetFinishComponent', () => {
  let component: PasswordResetFinishComponent;
  let fixture: ComponentFixture<PasswordResetFinishComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PasswordResetFinishComponent]
    });
    fixture = TestBed.createComponent(PasswordResetFinishComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
