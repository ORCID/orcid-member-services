import { HttpClient, HttpParams } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import { Observable, map } from 'rxjs'
import { Client } from '../model/client'

// Raw shape returned by the external api-credentials-service search endpoint
// (org.orcid.apicreds.dto.ClientDetailsSummaryDto) — not the same as the ui-2 Client model.
interface ClientDetailsSummaryDto {
  clientId: string
  clientName: string
  editable?: boolean
  homepageUrl?: string | null
  description?: string | null
  clientSecret?: string | null
  redirectUris?: string[] | null
}

// Raw shape returned by the upstream api-credentials-service or the gateway.
// (org.orcid.apicreds.dto.ClientDetailsDto) is normalized below when returned directly.
interface ClientDetailsDto {
  memberId: string
  clientDetailsId: string
  redirectUris: { redirectUri: string; redirectUriType?: string }[]
  name: string
  description?: string | null
  website?: string | null
  membershipType?: string
  decryptedSecret?: string | null
}

type ClientDetailsResponse = ClientDetailsDto | Client

interface RawPagedResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ClientSearchResult {
  content: Client[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

function toClientFromSummary(dto: ClientDetailsSummaryDto): Client {
  return {
    clientId: dto.clientId,
    clientName: dto.clientName,
    editable: dto.editable ?? true,
    homepageUrl: dto.homepageUrl ?? undefined,
    description: dto.description ?? undefined,
    clientSecret: dto.clientSecret ?? undefined,
    redirectUris: dto.redirectUris ?? undefined,
  }
}

function toClientFromDetails(dto: ClientDetailsResponse): Client {
  if ('clientId' in dto) {
    return {
      clientId: dto.clientId,
      clientName: dto.clientName,
      editable: dto.editable ?? true,
      homepageUrl: dto.homepageUrl ?? undefined,
      description: dto.description ?? undefined,
      clientSecret: dto.clientSecret ?? undefined,
      redirectUris: dto.redirectUris ?? undefined,
    }
  }

  return {
    clientId: dto.clientDetailsId,
    clientName: dto.name,
    editable: true,
    homepageUrl: dto.website ?? undefined,
    description: dto.description ?? undefined,
    clientSecret: dto.decryptedSecret ?? undefined,
    redirectUris: (dto.redirectUris ?? []).map((uri) => uri.redirectUri),
  }
}

function toClientDetailsDto(credential: Client): ClientDetailsDto {
  return {
    memberId: '',
    clientDetailsId: credential.clientId,
    name: credential.clientName,
    description: credential.description,
    website: credential.homepageUrl,
    redirectUris: (credential.redirectUris ?? []).map((redirectUri) => ({ redirectUri, redirectUriType: 'default' })),
  }
}

@Injectable({
  providedIn: 'root',
})
export class ApiCredentialsService {
  private http = inject(HttpClient)

  public resourceUrl = '/memberservice/api/clients'

  get(clientId: string): Observable<Client> {
    return this.http
      .get<ClientDetailsResponse>(`${this.resourceUrl}/${clientId}`)
      .pipe(map(toClientFromDetails))
  }

  create(credential: Client): Observable<Client> {
    return this.http
      .post<ClientDetailsResponse>(this.resourceUrl, toClientDetailsDto(credential))
      .pipe(map(toClientFromDetails))
  }

  update(credential: Client): Observable<Client> {
    return this.http
      .put<ClientDetailsResponse>(`${this.resourceUrl}/${credential.clientId}`, toClientDetailsDto(credential))
      .pipe(map(toClientFromDetails))
  }

  search(options?: { page?: number; size?: number; sort?: string; clientType?: string[] }): Observable<ClientSearchResult> {
    let params = new HttpParams()
    if (options?.page != null) {
      params = params.set('page', options.page)
    }
    if (options?.size != null) {
      params = params.set('size', options.size)
    }
    if (options?.sort) {
      params = params.set('sort', options.sort)
    }
    for (const clientType of options?.clientType ?? []) {
      params = params.append('clientType', clientType)
    }
    return this.http
      .get<RawPagedResult<ClientDetailsSummaryDto>>(this.resourceUrl, { params })
      .pipe(map((page) => ({ ...page, content: page.content.map(toClientFromSummary) })))
  }

  resetClientSecret(clientId: string): Observable<{ clientSecret: string }> {
    return this.http.post<{ clientSecret: string }>(`${this.resourceUrl}/${clientId}/reset-secret`, {})
  }
}

