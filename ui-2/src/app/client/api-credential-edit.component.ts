import { HttpErrorResponse } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormArray, FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { ClipboardModule } from 'ngx-clipboard'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import {
  faBan,
  faCopy,
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
import { CLIENT_DESCRIPTION_MAX_LENGTH, Client } from './model/client'
import { ApiCredentialsService } from './service/api-credentials.service'
import { ResetClientSecretDialogComponent } from './reset-client-secret-dialog.component'

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

  protected resetSecretEnabled = true

  protected isSaving = signal(false)
  protected invalidForm = signal(false)
  protected validationErrors = signal<string[]>([])
  protected isCreateMode = signal(false)
  protected clientSecretRevealed = signal(true)
  protected secretReset = signal(false)
  protected clientId = signal('')

  protected faBan = faBan
  protected faSave = faSave
  protected faCopy = faCopy
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
    homepageUrl: this.fb.control<string | null>(null, [Validators.required]),
    description: this.fb.control<string | null>(null, [Validators.required, Validators.maxLength(CLIENT_DESCRIPTION_MAX_LENGTH)]),
    clientSecret: this.fb.control<string | null>({ value: null, disabled: true }),
    redirectUris: new FormArray<FormControl<string | null>>([], [Validators.required]),
  })

  ngOnInit() {
    this.activatedRoute.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => {
      const credential = data['credential'] as Client | undefined
      if (credential) {
        this.updateForm(credential)
      } else {
        this.isCreateMode.set(true)
      }
    })
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
      this.redirectUris.push(new FormControl<string | null>(uri, [Validators.required]))
    })
  }

  addRedirectUri() {
    this.redirectUris.push(new FormControl<string | null>(null, [Validators.required]))
  }

  removeRedirectUri(index: number) {
    this.redirectUris.removeAt(index)
  }

  toggleSecret() {
    this.clientSecretRevealed.update((revealed) => !revealed)
  }

  displayClientSecret(): string {
    const secret = this.editForm.controls.clientSecret.value
    if (!secret) {
      return 'Client secret unavailable'
    }
    return this.clientSecretRevealed() ? secret : '•'.repeat(secret.length)
  }

  copySuccess() {
    this.alertService.broadcast(
      AlertType.TOAST,
      $localize`:@@apiCredentials.edit.copySuccess.string:Copied to clipboard`
    )
  }

  openResetDialog() {
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
    this.apiCredentialsService.resetClientSecret(this.clientId()).subscribe({
      next: (res) => {
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
    if (this.editForm.invalid) {
      this.invalidForm.set(true)
      this.editForm.markAllAsTouched()
      return
    }
    this.isSaving.set(true)
    this.invalidForm.set(false)
    this.validationErrors.set([])
    const value = this.editForm.getRawValue()
    const payload: Client = {
      clientId: this.clientId(),
      clientName: value.clientName,
      editable: true,
      homepageUrl: value.homepageUrl ?? undefined,
      description: value.description ?? undefined,
      redirectUris: value.redirectUris.filter((uri): uri is string => !!uri),
    }

    const request = this.isCreateMode() ? this.apiCredentialsService.create(payload) : this.apiCredentialsService.update(payload)
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
          this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_UPDATED)
          this.router.navigate(this.collectionRoute())
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

  private getErrorMessages(error: unknown): string[] {
    if (!(error instanceof HttpErrorResponse)) {
      return ['Unable to save this client. Please try again.']
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
    return typeof message === 'string' && message.trim() ? [message] : ['Unable to save this client. Please try again.']
  }
}
