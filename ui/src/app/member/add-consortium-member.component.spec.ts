import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddConsortiumMemberComponent } from './add-consortium-member.component';

describe('AddConsortiumMemberComponent', () => {
  let component: AddConsortiumMemberComponent;
  let fixture: ComponentFixture<AddConsortiumMemberComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AddConsortiumMemberComponent]
    });
    fixture = TestBed.createComponent(AddConsortiumMemberComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
