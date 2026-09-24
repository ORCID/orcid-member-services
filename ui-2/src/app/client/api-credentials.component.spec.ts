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
    const apiCredentialsServiceSpy = jasmine.createSpyObj('ApiCredentialsService', ['getClientsForMember'])
    apiCredentialsServiceSpy.getClientsForMember.and.returnValue(of(mockClients))
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

  it('should populate production credentials from the clients of the route member', () => {
    expect(apiCredentialsService.getClientsForMember).toHaveBeenCalledWith('memberId')
    expect(internals(component).productionCredentials()).toEqual(mockClients)
  })

  it('should keep sandbox credentials empty when search fails', () => {
    apiCredentialsService.getClientsForMember.and.returnValue(throwError(() => new Error('failed')))
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    expect(internals(component).sandboxCredentials()).toEqual([])
  })

  it('should not call search when no route member id is available', () => {
    const activatedRoute = TestBed.inject(ActivatedRoute)
    activatedRoute.snapshot = { paramMap: convertToParamMap({}) } as ActivatedRoute['snapshot']
    apiCredentialsService.getClientsForMember.calls.reset()
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    expect(apiCredentialsService.getClientsForMember).not.toHaveBeenCalled()
  })
})


