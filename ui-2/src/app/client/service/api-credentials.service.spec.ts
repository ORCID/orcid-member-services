import { TestBed } from '@angular/core/testing'
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing'
import { ApiCredentialsService } from './api-credentials.service'
import { Client } from '../model/client'

describe('ApiCredentialsService', () => {
  let service: ApiCredentialsService
  let httpMock: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    })
    service = TestBed.inject(ApiCredentialsService)
    httpMock = TestBed.inject(HttpTestingController)
  })

  afterEach(() => {
    httpMock.verify()
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })

  it('should get a client by id and map the ClientDetailsDto response', () => {
    const dto = {
      memberId: 'memberId',
      clientDetailsId: 'abc123',
      name: 'Test',
      description: 'A test client',
      website: 'https://example.org',
      decryptedSecret: 'secret',
      redirectUris: [{ redirectUri: 'https://example.org/callback', redirectUriType: 'default' }],
    }

    service.get('abc123').subscribe((res) => {
      expect(res).toEqual({
        clientId: 'abc123',
        clientName: 'Test',
        editable: true,
        homepageUrl: 'https://example.org',
        description: 'A test client',
        clientSecret: 'secret',
        redirectUris: ['https://example.org/callback'],
      })
    })

    const req = httpMock.expectOne(`${service.resourceUrl}/abc123`)
    expect(req.request.method).toBe('GET')
    req.flush(dto)
  })

  it('should preserve a gateway-shaped client detail response', () => {
    const gatewayClient = {
      clientName: 'Orcid MCP',
      clientId: 'APP-2W7DNBN7X5B2EAF9',
      editable: true,
      homepageUrl: 'orcid.org',
      description: 'Orcid MCP testing area',
      clientSecret: null,
      redirectUris: ['orcid.org'],
    }

    service.get(gatewayClient.clientId).subscribe((res) => {
      expect(res).toEqual({ ...gatewayClient, clientSecret: undefined })
    })

    const req = httpMock.expectOne(`${service.resourceUrl}/${gatewayClient.clientId}`)
    expect(req.request.method).toBe('GET')
    req.flush(gatewayClient)
  })

  it('should update a client, sending the ClientDetailsDto request shape', () => {
    const credential = {
      clientId: 'abc123',
      clientName: 'Test',
      editable: true,
      homepageUrl: 'https://example.org',
      redirectUris: ['https://example.org/callback'],
    } as Client
    const updated = { memberId: 'memberId', clientDetailsId: 'abc123', name: 'Test', redirectUris: [] }

    service.update(credential).subscribe((res) => {
      expect(res.clientId).toEqual('abc123')
      expect(res.clientName).toEqual('Test')
    })

    const req = httpMock.expectOne(`${service.resourceUrl}/abc123`)
    expect(req.request.method).toBe('PUT')
    expect(req.request.body.clientDetailsId).toEqual('abc123')
    expect(req.request.body.name).toEqual('Test')
    expect(req.request.body.website).toEqual('https://example.org')
    expect(req.request.body.redirectUris).toEqual([{ redirectUri: 'https://example.org/callback', redirectUriType: 'default' }])
    req.flush(updated)
  })

  it('should create a client, sending the ClientDetailsDto request shape', () => {
    const credential = { clientName: 'Test' } as Client
    const created = { memberId: 'memberId', clientDetailsId: 'abc123', name: 'Test', decryptedSecret: 'secret', redirectUris: [] }

    service.create(credential).subscribe((res) => {
      expect(res.clientId).toEqual('abc123')
      expect(res.clientSecret).toEqual('secret')
    })

    const req = httpMock.expectOne(service.resourceUrl)
    expect(req.request.method).toBe('POST')
    expect(req.request.body.name).toEqual('Test')
    req.flush(created)
  })

  it('should search clients without a member query parameter and map the response', () => {
    const summary = {
      clientId: 'abc123',
      clientName: 'Test',
      editable: true,
    }
    const rawPage = { content: [summary], totalElements: 1, totalPages: 1, number: 0, size: 20 }

    service.search({ page: 0, size: 20, sort: 'dateCreated,DESC', clientType: ['UPDATER'] }).subscribe((res) => {
      expect(res.content).toEqual([
        {
          clientId: 'abc123',
          clientName: 'Test',
          editable: true,
          homepageUrl: undefined,
          description: undefined,
          clientSecret: undefined,
          redirectUris: undefined,
        },
      ])
      expect(res.totalElements).toBe(1)
    })

    const req = httpMock.expectOne(
      (r) =>
        r.url === service.resourceUrl &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '20'
    )
    expect(req.request.method).toBe('GET')
    expect(req.request.params.get('sort')).toBe('dateCreated,DESC')
    expect(req.request.params.getAll('clientType')).toEqual(['UPDATER'])
    req.flush(rawPage)
  })

  it('should mark a client as non-editable when it has been deactivated', () => {
    const summary = {
      clientId: 'abc123',
      clientName: 'Test',
      editable: false,
    }

    service.search().subscribe((res) => {
      expect(res.content[0].editable).toBeFalse()
    })

    const req = httpMock.expectOne((r) => r.url === service.resourceUrl && r.params.keys().length === 0)
    req.flush({ content: [summary], totalElements: 1, totalPages: 1, number: 0, size: 20 })
  })

  it('should reset a client secret', () => {
    service.resetClientSecret('abc123').subscribe((res) => {
      expect(res).toEqual({ clientSecret: 'newSecret' })
    })

    const req = httpMock.expectOne(`${service.resourceUrl}/abc123/reset-secret`)
    expect(req.request.method).toBe('POST')
    req.flush({ clientSecret: 'newSecret' })
  })
})
