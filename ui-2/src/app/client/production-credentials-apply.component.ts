import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { ActivatedRoute, Router } from '@angular/router'
import { FaIconComponent } from '@fortawesome/angular-fontawesome'
import { faBan, faSave } from '@fortawesome/free-solid-svg-icons'
import { AlertMessage, AlertType } from '../app.constants'
import { AlertService } from '../shared/service/alert.service'
import { CLIENT_DESCRIPTION_MAX_LENGTH } from './model/client'

// TODO: values are placeholders (TBD) — confirm the real enum/labels with the product owner.
interface IntegrationTypeOption {
  value: string
  label: string
  helpLink?: string
}

@Component({
  selector: 'app-production-credentials-apply',
  templateUrl: './production-credentials-apply.component.html',
  styleUrls: ['./production-credentials-apply.component.scss'],
  imports: [ReactiveFormsModule, FaIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductionCredentialsApplyComponent {
  private fb = inject(NonNullableFormBuilder)
  private router = inject(Router)
  private activatedRoute = inject(ActivatedRoute)
  private alertService = inject(AlertService)
  private destroyRef = inject(DestroyRef)

  protected readonly descriptionMaxLength = CLIENT_DESCRIPTION_MAX_LENGTH

  protected faBan = faBan
  protected faSave = faSave

  private collectionRoute(): string[] {
    const memberId = this.activatedRoute.parent?.snapshot.paramMap.get('memberId')
    return memberId ? ['/api-credentials', memberId] : ['/']
  }

  // Drives the conditional Redirect URIs block in the template.
  protected showRedirectUris = signal(true)

  // TODO(TBD): confirm option values/labels.
  protected integrationTypeOptions: IntegrationTypeOption[] = [
    {
      value: 'IN_HOUSE',
      label: $localize`:@@apiCredentials.apply.integrationType.inHouse.string:In-house/bespoke/custom integration`,
    },
    {
      value: 'VENDOR',
      label: $localize`:@@apiCredentials.apply.integrationType.vendor.string:Integration for a system built by the vendor/service provider`,
    },
    {
      value: 'CSP',
      label: $localize`:@@apiCredentials.apply.integrationType.csp.string:Integration for an ORCID Certified Service Provider (CSP)`,
      helpLink: 'https://info.orcid.org/vendors-and-service-providers/orcid-certified-service-providers-list/',
    },
  ]

  applyForm = this.fb.group({
    integrationType: this.fb.control<string | null>(null, [Validators.required]),
    authenticateOrcidIds: this.fb.control<'YES' | 'NO' | null>(null, [Validators.required]),
    displayName: this.fb.control<string>('', [Validators.required]),
    homepageUrl: this.fb.control<string | null>(null, [Validators.required]),
    description: this.fb.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(CLIENT_DESCRIPTION_MAX_LENGTH),
    ]),
    redirectUris: this.fb.control<string | null>(null, [Validators.required]),
    notes: this.fb.control<string | null>(null),
  })

  constructor() {
    // NO answer => hide Redirect URIs and drop its requirement; YES => show + require.
    this.applyForm.controls.authenticateOrcidIds.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((answer) => {
        const control = this.applyForm.controls.redirectUris
        if (answer === 'NO') {
          this.showRedirectUris.set(false)
          control.reset(null)
          control.clearValidators()
        } else {
          this.showRedirectUris.set(true)
          control.setValidators([Validators.required])
        }
        control.updateValueAndValidity()
      })
  }

  save() {
    if (this.applyForm.invalid) {
      this.applyForm.markAllAsTouched()
      return
    }
    const value = this.applyForm.getRawValue()
    // Frontend-only for now: no submission endpoint is wired yet.
    // Redirect URIs are entered one-per-line; split for the future payload.
    const payload = {
      ...value,
      redirectUris: (value.redirectUris ?? '')
        .split('\n')
        .map((uri) => uri.trim())
        .filter((uri) => uri.length > 0),
    }
    // eslint-disable-next-line no-console
    console.log('Production credentials application submitted', payload)
    this.alertService.broadcast(AlertType.TOAST, AlertMessage.API_CREDENTIAL_APPLICATION_SUBMITTED)
    this.router.navigate(this.collectionRoute())
  }

  cancel() {
    this.router.navigate(this.collectionRoute())
  }
}
