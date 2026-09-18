import { ComponentFixture, TestBed } from '@angular/core/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ActivatedRoute, convertToParamMap, RouterModule } from '@angular/router'
import { of, throwError } from 'rxjs'

import { ApiCredentialsComponent } from './api-credentials.component'
import { ApiCredentialsService } from './service/api-credentials.service'
import { Client } from './model/client'

type ApiCredentialsComponentInternals = {
  productionCredentials: () => Client[]
  sandboxCredentials: () => Client[]
}

const internals = (component: ApiCredentialsComponent): ApiCredentialsComponentInternals =>
  component as unknown as ApiCredentialsComponentInternals

describe('ManageApiCredentialsComponent', () => {
  let component: ApiCredentialsComponent
  let fixture: ComponentFixture<ApiCredentialsComponent>
  let apiCredentialsService: jasmine.SpyObj<ApiCredentialsService>

  const mockClients: Client[] = [{ clientId: 'abc123', clientName: 'Test', editable: true }]

  beforeEach(() => {
    const apiCredentialsServiceSpy = jasmine.createSpyObj('ApiCredentialsService', ['search'])
    apiCredentialsServiceSpy.search.and.returnValue(
      of({ content: mockClients, totalElements: 1, totalPages: 1, number: 0, size: 20 })
    )
    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), ApiCredentialsComponent],
      providers: [
        { provide: ApiCredentialsService, useValue: apiCredentialsServiceSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ memberId: 'memberId' }) } } },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
    apiCredentialsService = TestBed.inject(ApiCredentialsService) as jasmine.SpyObj<ApiCredentialsService>
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should populate production credentials from search results without sending a member id', () => {
    expect(apiCredentialsService.search).toHaveBeenCalledWith()
    expect(internals(component).productionCredentials()).toEqual(mockClients)
  })

  it('should keep sandbox credentials empty when search fails', () => {
    apiCredentialsService.search.and.returnValue(throwError(() => new Error('failed')))
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    expect(internals(component).sandboxCredentials()).toEqual([])
  })

  it('should not call search when no route member id is available', () => {
    const activatedRoute = TestBed.inject(ActivatedRoute)
    activatedRoute.snapshot = { paramMap: convertToParamMap({}) } as ActivatedRoute['snapshot']
    apiCredentialsService.search.calls.reset()
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    expect(apiCredentialsService.search).not.toHaveBeenCalled()
  })
})


