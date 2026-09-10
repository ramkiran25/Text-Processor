import { TestBed } from '@angular/core/testing';
import { LoginModel } from './login.model';

describe('LoginModel', () => {
  let service: LoginModel;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoginModel);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
