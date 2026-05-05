import { ComponentFixture, TestBed } from '@angular/core/testing'

import { ActivatedRoute } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'
import { of } from 'rxjs'
import { AccountService } from '../account'
import { ErrorComponent } from './error.component'

describe('ErrorComponent', () => {
  let component: ErrorComponent
  let fixture: ComponentFixture<ErrorComponent>
  let accountServiceSpy: jasmine.SpyObj<AccountService>

  beforeEach(() => {
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['isAuthenticated'])

    TestBed.configureTestingModule({
      declarations: [ErrorComponent],
      imports: [RouterTestingModule],
      providers: [
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: ActivatedRoute, useValue: { data: of({}) } },
      ],
    })
    fixture = TestBed.createComponent(ErrorComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
