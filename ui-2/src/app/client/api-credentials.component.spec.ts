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

  const mockClients: Client[] = [{ clientId: 'abc123', clientName: 'Test', editable: true, slClient: false }]

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

  it('should link S&L clients to request changes and only offer Edit on editable typed clients', () => {
    apiCredentialsService.getClientsForMember.and.returnValue(
      of([
        { clientId: 'SL-1', clientName: 'S&L', editable: true, slClient: true },
        { clientId: 'N-1', clientName: 'Normal', editable: true, slClient: false },
        { clientId: 'N-2', clientName: 'Inactive', editable: false, slClient: false },
        { clientId: 'N-3', clientName: 'Untyped', editable: true },
      ])
    )
    fixture = TestBed.createComponent(ApiCredentialsComponent)
    fixture.detectChanges()

    const rows: HTMLElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.credentials-table')[0].querySelectorAll('tbody tr')
    )
    const action = (row: HTMLElement) => row.querySelector('.client-actions a')

    expect(action(rows[0])?.textContent).toContain('Request changes')
    expect(action(rows[0])?.getAttribute('href')).toBe('/api-credentials/memberId/SL-1')
    expect(action(rows[1])?.textContent).toContain('Edit')
    expect(action(rows[2])).toBeNull()
    expect(action(rows[3])).toBeNull()
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
