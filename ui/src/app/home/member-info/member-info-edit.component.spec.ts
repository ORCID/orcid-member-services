import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MemberInfoEditComponent } from './member-info-edit.component';

describe('MemberInfoEditComponent', () => {
  let component: MemberInfoEditComponent;
  let fixture: ComponentFixture<MemberInfoEditComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MemberInfoEditComponent]
    });
    fixture = TestBed.createComponent(MemberInfoEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
