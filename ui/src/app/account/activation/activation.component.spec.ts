import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing'

import { ActivationComponent } from './activation.component'
import { ActivationService } from './activation.service'
import { ActivatedRoute } from '@angular/router'
import { async, of, throwError } from 'rxjs'
import { RouterTestingModule } from '@angular/router/testing'

describe('ActivationComponent', () => {
  let component: ActivationComponent
  let fixture: ComponentFixture<ActivationComponent>
  let activationServiceSpy: jasmine.SpyObj<ActivationService>

  beforeEach(() => {
    activationServiceSpy = jasmine.createSpyObj('ActivationService', ['get'])

    TestBed.configureTestingModule({
      declarations: [ActivationComponent],
      imports: [RouterTestingModule],
      providers: [{ provide: ActivationService, useValue: activationServiceSpy }],
    }).compileComponents()

    fixture = TestBed.createComponent(ActivationComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    activationServiceSpy = TestBed.inject(ActivationService) as jasmine.SpyObj<ActivationService>
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('non-error response from server when sending key should indicate success', () => {
    activationServiceSpy.get.and.returnValue(of({}))
    component.ngOnInit()
    expect(component.success).toBeTruthy()
    expect(component.error).toBeFalsy()
  })

  it('error response from server when sending key should indicate failure', () => {
    activationServiceSpy.get.and.returnValue(throwError(() => new Error('error')))
    component.ngOnInit()

    expect(component.success).toBeFalsy()
    expect(component.error).toBeTruthy()
  })
})

export class MockActivatedRoute extends ActivatedRoute {
  constructor() {
    super()
    this.queryParams = of({ key: 'key' })
  }
}
