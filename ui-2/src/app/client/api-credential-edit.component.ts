import { HttpErrorResponse } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { catchError, map, of, switchMap } from 'rxjs'
import { FormArray, FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { ClipboardModule } from 'ngx-clipboard'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import {
  faBan,
  faCopy,
  faEnvelope,
  faEye,
  faEyeSlash,
  faLock,
  faPlus,
  faRedo,
  faSave,
  faTrashAlt,
} from '@fortawesome/free-solid-svg-icons'
import { AlertMessage, AlertType } from '../app.constants'
import { AlertService } from '../shared/service/alert.service'
import { CLIENT_DESCRIPTION_MAX_LENGTH, CLIENT_UPDATED_STATE_KEY, Client, isSlClient } from './model/client'
import { SlClientChangeRequest } from './model/sl-client-change-request'
import { ApiCredentialsService } from './service/api-credentials.service'
import { ResetClientSecretDialogComponent } from './reset-client-secret-dialog.component'
import { redirectUriValidator, websiteUrlValidator } from './validators/url.validators'

@Component({
  selector: 'app-api-credential-edit',
  templateUrl: './api-credential-edit.component.html',
  styleUrls: ['./api-credential-edit.component.scss'],
  imports: [ReactiveFormsModule, FaIconComponent, ClipboardModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiCredentialEditComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute)
  protected router = inject(Router)
  protected apiCredentialsService = inject(ApiCredentialsService)
  private fb = inject(NonNullableFormBuilder)
  private alertService = inject(AlertService)
  private modalService = inject(NgbModal)
  private destroyRef = inject(DestroyRef)

  protected readonly descriptionMaxLength = CLIENT_DESCRIPTION_MAX_LENGTH

  protected isSaving = signal(false)
  protected invalidForm = signal(false)
  protected validationErrors = signal<string[]>([])
  protected isCreateMode = signal(false)
  protected isRequestMode = signal(false)
  protected accessBlocked = signal(false)
  protected clientSecretRevealed = signal(true)
  protected isLoadingSecret = signal(false)
  protected secretReset = signal(false)
  protected previousClientSecret = signal<string | null>(null)
  protected clientId = signal('')

  protected faBan = faBan
  protected faSave = faSave
  protected faCopy = faCopy
  protected faEnvelope = faEnvelope
  protected faEye = faEye
  protected faEyeSlash = faEyeSlash
  protected faLock = faLock
  protected faPlus = faPlus
  protected faTrashAlt = faTrashAlt
  protected faRedo = faRedo

  private collectionRoute(): string[] {
    const memberId = this.activatedRoute.snapshot.paramMap.get('memberId')
    return memberId ? ['/api-credentials', memberId] : ['/']
  }

  editForm = this.fb.group({
    clientName: this.fb.control<string>('', [Validators.required]),
    homepageUrl: this.fb.control<string | null>(null, [Validators.required, websiteUrlValidator()]),
    description: this.fb.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(CLIENT_DESCRIPTION_MAX_LENGTH),
    ]),
    clientSecret: this.fb.control<string | null>({ value: null, disabled: true }),
    redirectUris: new FormArray<FormControl<string | null>>([], [Validators.required]),
  })

  ngOnInit() {
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => {
      const credential = data['credential'] as Client | undefined
      if (credential) {
        this.applyAccess(credential)
        this.updateForm(credential)
        this.clientSecretRevealed.set(false)
      } else {
        this.isCreateMode.set(true)
      }
    })
  }

  // Only S&L clients go through the email request flow; a client with an unknown type must not be edited directly.
  private applyAccess(credential: Client) {
    const slClient = isSlClient(credential)
    this.isRequestMode.set(slClient)
    if (slClient) {
      this.clientSecretRevealed.set(false)
      // the S&L change request email only carries the homepage URL and redirect URIs
      this.editForm.controls.description.disable()
    } else if (credential.slClient === undefined || !credential.editable) {
      this.accessBlocked.set(true)
      this.validationErrors.set(['This client cannot be edited.'])
    }
  }

  protected canResetSecret(): boolean {
    return !this.isRequestMode() && !this.accessBlocked()
  }

  get redirectUris(): FormArray<FormControl<string | null>> {
    return this.editForm.controls.redirectUris
  }

  updateForm(credential: Client) {
    this.clientId.set(credential.clientId)
    this.editForm.patchValue({
      clientName: credential.clientName,
      homepageUrl: credential.homepageUrl ?? null,
      description: credential.description ?? null,
      clientSecret: credential.clientSecret ?? null,
    })
    // the client name is immutable once a client has been created
    this.editForm.controls.clientName.disable()

    this.redirectUris.clear()
    ;(credential.redirectUris ?? []).forEach((uri) => {
      this.redirectUris.push(new FormControl<string | null>(uri, [Validators.required, redirectUriValidator()]))
    })
  }

  addRedirectUri() {
    this.redirectUris.push(new FormControl<string | null>(null, [Validators.required, redirectUriValidator()]))
  }

  removeRedirectUri(index: number) {
    this.redirectUris.removeAt(index)
  }

  toggleSecret() {
    if (this.clientSecretRevealed()) {
      this.clientSecretRevealed.set(false)
      return
    }
    if (this.editForm.controls.clientSecret.value) {
      this.clientSecretRevealed.set(true)
      return
    }
    this.isLoadingSecret.set(true)
    this.apiCredentialsService.getClientSecret(this.clientId()).subscribe({
      next: (res) => {
        this.isLoadingSecret.set(false)
        this.editForm.controls.clientSecret.setValue(res.clientSecret)
        this.clientSecretRevealed.set(true)
      },
      error: (error: unknown) => {
        this.isLoadingSecret.set(false)
        const notFound = error instanceof HttpErrorResponse && error.status === 404
        this.alertService.broadcast(
          AlertType.TOAST,
          notFound ? AlertMessage.API_CREDENTIAL_SECRET_NOT_FOUND : AlertMessage.API_CREDENTIAL_SECRET_LOAD_ERROR
        )
      },
    })
  }

  displayClientSecret(): string {
    const secret = this.editForm.controls.clientSecret.value
    if (!this.clientSecretRevealed()) {
      return '•'.repeat(secret?.length || 36)
    }
    return secret || 'Client secret unavailable'
  }

  copySuccess() {
    this.alertService.broadcast(
      AlertType.TOAST,
      $localize`:@@apiCredentials.edit.copySuccess.string:Copied to clipboard`
    )
  }

  openResetDialog() {
    if (!this.canResetSecret()) {
      return
    }
    const ref = this.modalService.open(ResetClientSecretDialogComponent, { size: 'lg' })
    ref.componentInstance.clientId = this.clientId()
    ref.componentInstance.currentSecret = this.editForm.controls.clientSecret.value ?? ''
    ref.result.then(
      (result: string) => {
        if (result === 'confirmed') {
          this.performReset()
        }
      },
      () => {
        // dialog dismissed without confirming, nothing to do
      }
    )
  }

  performReset() {
    if (!this.canResetSecret()) {
      return
    }
    const clientId = this.clientId()
    const loadedSecret = this.editForm.controls.clientSecret.value
    // the notice names the secret being replaced, so fetch it first if it was never revealed
    const previousSecret$ = loadedSecret
      ? of<string | null>(loadedSecret)
      : this.apiCredentialsService.getClientSecret(clientId).pipe(
          map((res): string | null => res.clientSecret),
          catchError(() => of(null))
        )

    previousSecret$
      .pipe(
        switchMap((previous) =>
          this.apiCredentialsService.resetClientSecret(clientId).pipe(map((res) => ({ previous, res })))
        )
      )
      .subscribe({
        next: ({ previous, res }) => {
          this.previousClientSecret.set(previous)
          this.editForm.controls.clientSecret.setValue(res.clientSecret)
          this.clientSecretRevealed.set(true)
          this.secretReset.set(true)
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_SECRET_RESET)
        },
        error: (error: unknown) => {
          this.validationErrors.set(this.getErrorMessages(error))
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_SAVE_ERROR)
        },
      })
  }

  save() {
    if (this.isSaving() || this.accessBlocked()) {
      return
    }
    if (this.editForm.invalid) {
      this.invalidForm.set(true)
      this.editForm.markAllAsTouched()
      return
    }
    this.isSaving.set(true)
    this.invalidForm.set(false)
    this.validationErrors.set([])
    const value = this.editForm.getRawValue()
    const redirectUris = value.redirectUris.map((uri) => uri?.trim()).filter((uri): uri is string => !!uri)

    if (this.isRequestMode()) {
      this.requestChanges({
        homepageUrl: value.homepageUrl?.trim() ?? '',
        redirectUris,
      })
      return
    }

    const payload: Client = {
      clientId: this.clientId(),
      clientName: value.clientName,
      editable: true,
      homepageUrl: value.homepageUrl?.trim() ?? undefined,
      description: value.description ?? undefined,
      redirectUris,
    }

    const request = this.isCreateMode()
      ? this.apiCredentialsService.create(payload)
      : this.apiCredentialsService.update(payload)
    request.subscribe({
      next: (result) => {
        this.isSaving.set(false)
        this.validationErrors.set([])
        if (this.isCreateMode()) {
          this.isCreateMode.set(false)
          this.updateForm(result)
          this.clientSecretRevealed.set(true)
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_CREATED)
        } else {
          this.router.navigate(this.collectionRoute(), {
            state: { [CLIENT_UPDATED_STATE_KEY]: result?.clientName || value.clientName },
          })
        }
      },
      error: (error: unknown) => {
        this.isSaving.set(false)
        this.validationErrors.set(this.getErrorMessages(error))
        this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_SAVE_ERROR)
      },
    })
  }

  cancel() {
    this.router.navigate(this.collectionRoute())
  }

  private requestChanges(request: SlClientChangeRequest) {
    this.apiCredentialsService.requestSLClientChange(request).subscribe({
      next: () => {
        this.isSaving.set(false)
        this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_CHANGE_REQUESTED)
        this.router.navigate(this.collectionRoute())
      },
      error: (error: unknown) => {
        this.isSaving.set(false)
        this.validationErrors.set(
          this.getErrorMessages(error, 'Unable to submit the change request. Please try again.')
        )
        this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_CHANGE_REQUEST_ERROR)
      },
    })
  }

  private getErrorMessages(error: unknown, fallback = 'Unable to save this client. Please try again.'): string[] {
    if (!(error instanceof HttpErrorResponse)) {
      return [fallback]
    }

    const body = error.error
    if (Array.isArray(body?.errors)) {
      const messages = body.errors.filter((message: unknown): message is string => typeof message === 'string')
      if (messages.length > 0) {
        return messages
      }
    }

    if (typeof body === 'string' && body.trim()) {
      try {
        const parsed = JSON.parse(body) as { errors?: unknown; detail?: unknown; message?: unknown }
        if (Array.isArray(parsed.errors)) {
          const messages = parsed.errors.filter((message: unknown): message is string => typeof message === 'string')
          if (messages.length > 0) {
            return messages
          }
        }
        if (typeof parsed.detail === 'string' && parsed.detail.trim()) {
          return [parsed.detail]
        }
        if (typeof parsed.message === 'string' && parsed.message.trim()) {
          return [parsed.message]
        }
      } catch {
        return [body]
      }
    }

    const message = body?.detail || body?.message || body?.title
    return typeof message === 'string' && message.trim() ? [message] : [fallback]
  }
}
