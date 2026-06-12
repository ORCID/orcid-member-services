import { TestBed } from '@angular/core/testing';
import { ApiCredentialsService } from './api-credentials.service';


describe('ApiCredentialsService', () => {
  let service: ApiCredentialsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ApiCredentialsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
