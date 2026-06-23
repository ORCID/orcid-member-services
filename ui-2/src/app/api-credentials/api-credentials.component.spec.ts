import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { ApiCredentialsComponent } from './api-credentials.component';

describe('ManageApiCredentialsComponent', () => {
  let component: ApiCredentialsComponent;
  let fixture: ComponentFixture<ApiCredentialsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ApiCredentialsComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });
    fixture = TestBed.createComponent(ApiCredentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
