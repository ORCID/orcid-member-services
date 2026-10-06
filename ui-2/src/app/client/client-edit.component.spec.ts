import { ComponentFixture, TestBed } from '@angular/core/testing'
import { HttpErrorResponse } from '@angular/common/http'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ReactiveFormsModule, FormArray, FormControl } from '@angular/forms'
import { ActivatedRoute, convertToParamMap, Router, RouterModule } from '@angular/router'
import { NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { of, Subject, throwError } from 'rxjs'
import { AlertMessage, AlertType } from '../app.constants'
import { AlertService } from '../shared/service/alert.service'
import { ApiCredentialEditComponent } from './api-credential-edit.component'
import { Client } from './model/client'
import { ApiCredentialsService } from './service/api-credentials.service'

type ApiCredentialEditComponentInternals = {
  clientId: () => string
  isCreateMode: () => boolean
  isRequestMode: () => boolean
  accessBlocked: () => boolean
  clientSecretRevealed: () => boolean
  secretReset: () => boolean
  invalidForm: () => boolean
  validationErrors: () => string[]
  redirectUris: FormArray<FormControl<string | null>>
}

const internals = (component: ApiCredentialEditComponent): ApiCredentialEditComponentInternals =>
  component as unknown as ApiCredentialEditComponentInternals

describe('ApiCredentialEditComponent', () => {
  let component: ApiCredentialEditComponent
  let fixture: ComponentFixture<ApiCredentialEditComponent>
  let apiCredentialsService: jasmine.SpyObj<ApiCredentialsService>
  let alertService: jasmine.SpyObj<AlertService>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>
  let router: jasmine.SpyObj<Router>

  const mockCredential: Client = {
    clientId: 'APP-ABC123',
    clientName: 'Test client',
    editable: true,
    slClient: false,
    homepageUrl: 'https://example.org',
    description: 'A test client',
    clientSecret: 'super-secret',
    redirectUris: ['https://example.org/callback'],
  }

  beforeEach(() => {
    const apiCredentialsServiceSpy = jasmine.createSpyObj('ApiCredentialsService', [
      'get',
      'create',
      'update',
      'resetClientSecret',
    ])
    const alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast'])
    const modalServiceSpy = jasmine.createSpyObj('NgbModal', ['open'])

    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), ReactiveFormsModule, ApiCredentialEditComponent],
      providers: [
        { provide: ApiCredentialsService, useValue: apiCredentialsServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
        { provide: NgbModal, useValue: modalServiceSpy },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })

    apiCredentialsService = TestBed.inject(ApiCredentialsService) as jasmine.SpyObj<ApiCredentialsService>
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>

    activatedRoute.data = of({ credential: mockCredential })
    activatedRoute.snapshot = {
      paramMap: convertToParamMap({ memberId: 'memberId', clientId: mockCredential.clientId }),
    } as ActivatedRoute['snapshot']
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true))

    fixture = TestBed.createComponent(ApiCredentialEditComponent)
    component = fixture.componentInstance
  })

  it('should create', () => {
    fixture.detectChanges()
    expect(component).toBeTruthy()
  })

  it('should populate the form from the resolved credential', () => {
    fixture.detectChanges()

    expect(component.editForm.get('homepageUrl')?.value).toBe('https://example.org')
    expect(component.editForm.get('description')?.value).toBe('A test client')
    expect(internals(component).clientId()).toBe('APP-ABC123')
    expect(internals(component).redirectUris.length).toBe(1)
  })

  it('should not call update when the form is invalid', () => {
    fixture.detectChanges()
    component.editForm.get('homepageUrl')?.setValue(null)

    component.save()

    expect(apiCredentialsService.update).not.toHaveBeenCalled()
    expect(internals(component).invalidForm()).toBe(true)
  })

  it('should show a useful server validation error when update fails', () => {
    fixture.detectChanges()
    apiCredentialsService.update.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { errors: ['Homepage URL cannot be empty'] },
          })
      )
    )

    component.save()

    expect(internals(component).validationErrors()).toEqual(['Homepage URL cannot be empty'])
  })

  it('should update and navigate back to the list on save', () => {
    fixture.detectChanges()
    apiCredentialsService.update.and.returnValue(of(mockCredential))

    component.save()

    expect(apiCredentialsService.update).toHaveBeenCalled()
    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.API_CREDENTIAL_UPDATED)
    expect(router.navigate).toHaveBeenCalledWith(['/api-credentials', 'memberId'])
  })

  it('should broadcast an error and stop saving when update fails', () => {
    fixture.detectChanges()
    apiCredentialsService.update.and.returnValue(throwError(() => new Error('failed')))

    component.save()

    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.API_CREDENTIAL_SAVE_ERROR)
    expect(router.navigate).not.toHaveBeenCalled()
  })

  it('should add and remove redirect URIs', () => {
    fixture.detectChanges()
    const initialLength = internals(component).redirectUris.length

    component.addRedirectUri()
    expect(internals(component).redirectUris.length).toBe(initialLength + 1)

    component.removeRedirectUri(0)
    expect(internals(component).redirectUris.length).toBe(initialLength)
  })

  it('should require at least one redirect URI', () => {
    fixture.detectChanges()

    component.removeRedirectUri(0)

    expect(internals(component).redirectUris.length).toBe(0)
    expect(internals(component).redirectUris.hasError('required')).toBeTrue()
  })

  it('should toggle the client secret visibility', () => {
    fixture.detectChanges()
    expect(internals(component).clientSecretRevealed()).toBe(true)

    component.toggleSecret()

    expect(internals(component).clientSecretRevealed()).toBe(false)
  })

  it('should reset the client secret', () => {
    fixture.detectChanges()
    apiCredentialsService.resetClientSecret.and.returnValue(of({ clientSecret: 'new-secret' }))

    component.performReset()

    expect(component.editForm.get('clientSecret')?.value).toBe('new-secret')
    expect(internals(component).secretReset()).toBe(true)
    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.API_CREDENTIAL_SECRET_RESET)
  })
})

describe('ApiCredentialEditComponent in create mode', () => {
  let component: ApiCredentialEditComponent
  let fixture: ComponentFixture<ApiCredentialEditComponent>
  let apiCredentialsService: jasmine.SpyObj<ApiCredentialsService>
  let alertService: jasmine.SpyObj<AlertService>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>

  const createdClient: Client = {
    clientId: 'APP-NEW123',
    clientName: 'New client',
    editable: true,
    clientSecret: 'brand-new-secret',
  }

  beforeEach(() => {
    const apiCredentialsServiceSpy = jasmine.createSpyObj('ApiCredentialsService', [
      'get',
      'create',
      'update',
      'resetClientSecret',
    ])
    const alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast'])
    const modalServiceSpy = jasmine.createSpyObj('NgbModal', ['open'])

    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), ReactiveFormsModule, ApiCredentialEditComponent],
      providers: [
        { provide: ApiCredentialsService, useValue: apiCredentialsServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
        { provide: NgbModal, useValue: modalServiceSpy },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })

    apiCredentialsService = TestBed.inject(ApiCredentialsService) as jasmine.SpyObj<ApiCredentialsService>
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>

    // the 'new' route has no resolver, so resolved data has no 'credential' key
    activatedRoute.data = of({})

    fixture = TestBed.createComponent(ApiCredentialEditComponent)
    component = fixture.componentInstance
  })

  it('should start in create mode with an empty, enabled client name field', () => {
    fixture.detectChanges()

    expect(internals(component).isCreateMode()).toBe(true)
    expect(component.editForm.controls.clientName.disabled).toBe(false)
  })

  it('should call create and reveal the secret in place on save', () => {
    fixture.detectChanges()
    component.editForm.patchValue({
      clientName: 'New client',
      homepageUrl: 'https://example.org',
      description: 'A new client',
    })
    component.addRedirectUri()
    component.redirectUris.at(0).setValue('https://example.org/callback')
    apiCredentialsService.create.and.returnValue(of(createdClient))

    component.save()

    expect(apiCredentialsService.create).toHaveBeenCalled()
    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.API_CREDENTIAL_CREATED)
    expect(internals(component).isCreateMode()).toBe(false)
    expect(internals(component).clientId()).toBe('APP-NEW123')
    expect(internals(component).clientSecretRevealed()).toBe(true)
    expect(component.editForm.get('clientSecret')?.value).toBe('brand-new-secret')
  })
})

describe('ApiCredentialEditComponent in S&L request mode', () => {
  let component: ApiCredentialEditComponent
  let fixture: ComponentFixture<ApiCredentialEditComponent>
  let apiCredentialsService: jasmine.SpyObj<ApiCredentialsService>
  let alertService: jasmine.SpyObj<AlertService>
  let modalService: jasmine.SpyObj<NgbModal>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>
  let router: jasmine.SpyObj<Router>

  const slCredential: Client = {
    clientId: 'APP-SL1',
    clientName: 'Search & Link Client',
    editable: true,
    slClient: true,
    homepageUrl: 'https://search-link.com',
    description: 'Search for your works',
    clientSecret: 'super-secret',
    redirectUris: ['https://search-link-redirect.com'],
  }

  const setup = (credential: Client) => {
    const apiCredentialsServiceSpy = jasmine.createSpyObj('ApiCredentialsService', [
      'create',
      'update',
      'resetClientSecret',
      'requestSLClientChange',
    ])
    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), ReactiveFormsModule, ApiCredentialEditComponent],
      providers: [
        { provide: ApiCredentialsService, useValue: apiCredentialsServiceSpy },
        { provide: AlertService, useValue: jasmine.createSpyObj('AlertService', ['broadcast']) },
        { provide: NgbModal, useValue: jasmine.createSpyObj('NgbModal', ['open']) },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
    apiCredentialsService = TestBed.inject(ApiCredentialsService) as jasmine.SpyObj<ApiCredentialsService>
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
    modalService = TestBed.inject(NgbModal) as jasmine.SpyObj<NgbModal>
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>
    activatedRoute.data = of({ credential })
    activatedRoute.snapshot = {
      paramMap: convertToParamMap({ memberId: 'memberId', clientId: credential.clientId }),
    } as ActivatedRoute['snapshot']
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true))
    fixture = TestBed.createComponent(ApiCredentialEditComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  }

  const text = () => fixture.nativeElement as HTMLElement

  it('should enter request mode, keep the name locked and mask the secret', () => {
    setup(slCredential)

    expect(internals(component).isRequestMode()).toBeTrue()
    expect(component.editForm.controls.clientName.disabled).toBeTrue()
    expect(component.editForm.controls.description.disabled).toBeTrue()
    expect(component.editForm.controls.description.value).toBe('Search for your works')
    expect(internals(component).clientSecretRevealed()).toBeFalse()
  })

  it('should show Request changes with the notice and no reset button', () => {
    setup(slCredential)

    expect(text().querySelector('#save-entity')?.textContent).toContain('Request changes')
    expect(text().querySelector('.reset-secret')).toBeNull()
    expect(text().querySelector('[data-cy="slRequestNotice"]')?.textContent).toContain('24 hours')
  })

  it('should not reset the secret from either handler', () => {
    setup(slCredential)

    component.openResetDialog()
    component.performReset()

    expect(modalService.open).not.toHaveBeenCalled()
    expect(apiCredentialsService.resetClientSecret).not.toHaveBeenCalled()
  })

  it('should send only the collated request and return to the list', () => {
    setup(slCredential)
    apiCredentialsService.requestSLClientChange.and.returnValue(of(undefined))
    component.editForm.patchValue({ homepageUrl: 'https://new.example.org' })
    component.addRedirectUri()
    component.redirectUris.at(1).setValue('https://new-redirect.org')

    component.save()

    expect(apiCredentialsService.requestSLClientChange).toHaveBeenCalledOnceWith({
      homepageUrl: 'https://new.example.org',
      redirectUris: ['https://search-link-redirect.com', 'https://new-redirect.org'],
    })
    expect(apiCredentialsService.update).not.toHaveBeenCalled()
    expect(apiCredentialsService.create).not.toHaveBeenCalled()
    expect(alertService.broadcast).toHaveBeenCalledWith(AlertType.TOAST, AlertMessage.API_CREDENTIAL_CHANGE_REQUESTED)
    expect(router.navigate).toHaveBeenCalledWith(['/api-credentials', 'memberId'])
  })

  it('should not submit an invalid form', () => {
    setup(slCredential)
    component.editForm.controls.homepageUrl.setValue(null)

    component.save()

    expect(apiCredentialsService.requestSLClientChange).not.toHaveBeenCalled()
    expect(internals(component).invalidForm()).toBeTrue()
  })

  it('should ignore a second submit while a request is in flight', () => {
    setup(slCredential)
    apiCredentialsService.requestSLClientChange.and.returnValue(new Subject<void>())

    component.save()
    component.save()

    expect(apiCredentialsService.requestSLClientChange).toHaveBeenCalledTimes(1)
  })

  it('should keep the form and allow retry when the request fails', () => {
    setup(slCredential)
    apiCredentialsService.requestSLClientChange.and.returnValue(throwError(() => new Error('failed')))

    component.save()

    expect(alertService.broadcast).toHaveBeenCalledWith(
      AlertType.TOAST,
      AlertMessage.API_CREDENTIAL_CHANGE_REQUEST_ERROR
    )
    expect(router.navigate).not.toHaveBeenCalled()
    expect(internals(component).validationErrors().length).toBe(1)
    expect(text().querySelector<HTMLButtonElement>('#save-entity')?.disabled).toBeFalse()
  })

  it('should not cancel through a request', () => {
    setup(slCredential)

    component.cancel()

    expect(apiCredentialsService.requestSLClientChange).not.toHaveBeenCalled()
    expect(router.navigate).toHaveBeenCalledWith(['/api-credentials', 'memberId'])
  })

  it('should block direct editing for a client with an unknown type', () => {
    setup({ ...slCredential, slClient: undefined })
    apiCredentialsService.update.and.returnValue(of(slCredential))

    component.save()
    component.performReset()

    expect(internals(component).isRequestMode()).toBeFalse()
    expect(internals(component).accessBlocked()).toBeTrue()
    expect(apiCredentialsService.update).not.toHaveBeenCalled()
    expect(apiCredentialsService.resetClientSecret).not.toHaveBeenCalled()
  })

  it('should show Save and the reset button for a normal editable client', () => {
    setup({ ...slCredential, slClient: false })

    expect(text().querySelector('#save-entity')?.textContent).toContain('Save')
    expect(text().querySelector('.reset-secret')).not.toBeNull()
    expect(text().querySelector('[data-cy="slRequestNotice"]')).toBeNull()
  })
})
