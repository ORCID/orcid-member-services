import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { HttpErrorResponse, HttpResponse } from '@angular/common/http'
import { interval } from 'rxjs'
import { KEYUTIL, KJUR, RSAKey } from 'jsrsasign'
import { LandingPageService } from './landing-page.service'
import { MemberService } from '../member/service/member.service'
import { IMember } from '../member/model/member.model'
import { BASE_URL, ORCID_BASE_URL } from '../app.constants'
import { WindowLocationService } from '../shared/service/window-location.service'
import { OrcidRecord } from '../shared/model/orcid-record.model'
import { ActivatedRoute } from '@angular/router'

@Component({
  selector: 'app-landing-page',
  templateUrl: './landing-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LandingPageComponent implements OnInit {
  private readonly landingPageService = inject(LandingPageService)
  private readonly windowLocationService = inject(WindowLocationService)
  protected readonly memberService = inject(MemberService)
  private readonly route = inject(ActivatedRoute)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly issuer = ORCID_BASE_URL
  protected readonly oauthBaseUrl = ORCID_BASE_URL + '/oauth/authorize'
  protected readonly redirectUri = BASE_URL + '/landing-page'

  protected readonly loadingState = signal(true)
  protected readonly showConnectionExistsState = signal(false)
  protected readonly showConnectionExistsDifferentUserState = signal(false)
  protected readonly showDeniedState = signal(false)
  protected readonly showErrorState = signal(false)
  protected readonly showSuccessState = signal(false)
  key: any
  protected readonly clientNameState = signal<string | undefined>(undefined)
  private readonly memberIdState = signal<string | undefined>(undefined)
  private readonly clientIdState = signal<string | undefined>(undefined)
  protected readonly oauthUrlState = signal<string | undefined>(undefined)
  protected readonly orcidRecordState = signal<OrcidRecord | undefined>(undefined)
  protected readonly signedInIdTokenState = signal<any>(undefined)
  private readonly givenNameState = signal<string | undefined>(undefined)
  private readonly familyNameState = signal<string | undefined>(undefined)
  private readonly progressbarValueState = signal(100)
  private readonly curSecState = signal(0)
  protected readonly incorrectDataMessageState = signal('')
  protected readonly linkAlreadyUsedMessageState = signal('')
  protected readonly allowToUpdateRecordMessageState = signal('')
  protected readonly successfullyGrantedMessageState = signal('')
  protected readonly thanksMessageState = signal('')

  protected get loading(): boolean {
    return this.loadingState()
  }

  protected get showConnectionExists(): boolean {
    return this.showConnectionExistsState()
  }

  protected get showConnectionExistsDifferentUser(): boolean {
    return this.showConnectionExistsDifferentUserState()
  }

  protected get showDenied(): boolean {
    return this.showDeniedState()
  }

  protected get showError(): boolean {
    return this.showErrorState()
  }

  protected get showSuccess(): boolean {
    return this.showSuccessState()
  }

  protected get clientName(): string | undefined {
    return this.clientNameState()
  }

  protected get oauthUrl(): string | undefined {
    return this.oauthUrlState()
  }

  protected get orcidRecord(): OrcidRecord | undefined {
    return this.orcidRecordState()
  }

  protected get signedInIdToken(): any {
    return this.signedInIdTokenState()
  }

  protected get incorrectDataMessage(): string {
    return this.incorrectDataMessageState()
  }

  protected get linkAlreadyUsedMessage(): string {
    return this.linkAlreadyUsedMessageState()
  }

  protected get allowToUpdateRecordMessage(): string {
    return this.allowToUpdateRecordMessageState()
  }

  protected get successfullyGrantedMessage(): string {
    return this.successfullyGrantedMessageState()
  }

  protected get thanksMessage(): string {
    return this.thanksMessageState()
  }

  ngOnInit() {
    const fragmentString = this.route.snapshot.fragment
    const fragmentParams = new URLSearchParams(fragmentString || '')

    // 2. Extract variables: check the fragment first, then fallback to query params (for URLs with '?')
    const state_param = fragmentParams.get('state') || this.route.snapshot.queryParamMap.get('state')
    const id_token_fragment = fragmentParams.get('id_token')
    const access_token_fragment = fragmentParams.get('access_token')

    if (state_param) {
      this.processRequest(state_param, id_token_fragment, access_token_fragment)
    }
  }

  protected processRequest(state_param: string, id_token_fragment: string | null, access_token_fragment: string | null) {
    this.landingPageService.getOrcidConnectionRecord(state_param).subscribe({
      next: (result) => {
        this.landingPageService.getMemberId(state_param).subscribe({
          next: (memberId) => {
            this.orcidRecordState.set(result)
            this.landingPageService.getMemberInfo(memberId).subscribe({
              next: (res: IMember) => {
                this.clientNameState.set(res.clientName)
                this.clientIdState.set(res.clientId)
                this.memberIdState.set(res.id)
                this.oauthUrlState.set(
                  this.oauthBaseUrl +
                  '?response_type=token&redirect_uri=' +
                  this.redirectUri +
                  '&client_id=' +
                  this.clientIdState() +
                  '&scope=/read-limited /activities/update /person/update openid&prompt=login&state=' +
                  state_param
                )

                this.incorrectDataMessageState.set(
                  $localize`:@@landingPage.success.ifYouFind.string:If you find that data added to your ORCID record is incorrect, please contact ${this.clientNameState()}`
                )
                this.linkAlreadyUsedMessageState.set(
                  $localize`:@@landingPage.connectionExists.differentUser.string:This authorization link has already been used. Please contact ${this.clientNameState()} for a new authorization link.`
                )
                this.allowToUpdateRecordMessageState.set(
                  $localize`:@@landingPage.denied.grantAccess.string:Allow ${this.clientNameState()} to update my ORCID record.`
                )
                this.successfullyGrantedMessageState.set(
                  $localize`:@@landingPage.success.youHaveSuccessfully.string:You have successfully granted ${this.clientNameState()} permission to update your ORCID record, and your record has been updated with affiliation information.`
                )

                // Check if id token exists in URL (user just granted permission)
                if (id_token_fragment && id_token_fragment) {
                  this.checkSubmitToken(id_token_fragment, state_param, access_token_fragment!)
                } else {
                  const fragmentString = this.route.snapshot.fragment
                  const fragmentParams = new URLSearchParams(fragmentString || '')
                  const error = fragmentParams.get('error')

                  // Check if user denied permission
                  if (error != null && error !== '') {
                    if (error === 'access_denied') {
                      this.submitUserDenied(state_param)
                    } else {
                      this.showErrorElement(error)
                    }
                  } else {
                    const oauthUrl = this.oauthUrlState()
                    if (oauthUrl) {
                      this.windowLocationService.updateWindowLocation(oauthUrl)
                    } else {
                      this.showErrorElement('OAuth URL is unavailable')
                    }
                  }
                }

                this.startTimer(600)
              },
              error: (err: HttpErrorResponse) => {
                console.error('Error fetching member info for member id:', memberId, err)
                this.showErrorElement(err)
              },
            })
          },
          error: (err: HttpErrorResponse) => {
            console.error('Error fetching member id for state param:', state_param, err)
            this.showErrorElement(err)
          },
        })
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error fetching orcid connection record for state param:', state_param, 'Error:', err)
        this.showErrorElement(err)
      },
    })
  }

  protected checkSubmitToken(id_token: string, state: string, access_token: string) {
    this.landingPageService.getPublicKey().subscribe({
      next: (res) => {
        const pubKey = KEYUTIL.getKey(res.keys[0]) as RSAKey
        const response = KJUR.jws.JWS.verifyJWT(id_token, pubKey, {
          alg: ['RS256'],
          iss: [this.issuer],
          aud: [this.clientIdState() || ''],
          gracePeriod: 15 * 60, // 15 mins skew allowed
        })
        if (response === true) {
          // check if existing token belongs to a different user

          this.landingPageService.submitUserResponse({ id_token, state, member_id: this.memberIdState() }).subscribe({
            next: (res) => {
              const data = res
              if (data) {
                if (data.isDifferentUser) {
                  this.showConnectionExistsDifferentUserElement()
                  return
                }
                if (data.isSameUserThatAlreadyGranted) {
                  this.showConnectionExistsElement()
                  return
                }
              }
              this.landingPageService.getUserInfo(access_token).subscribe({
                next: (result: HttpResponse<any>) => {
                  this.signedInIdTokenState.set(result)
                  this.givenNameState.set('')
                  if (this.signedInIdTokenState()?.given_name) {
                    this.givenNameState.set(this.signedInIdTokenState().given_name)
                  }
                  this.familyNameState.set('')
                  if (this.signedInIdTokenState()?.family_name) {
                    this.familyNameState.set(this.signedInIdTokenState().family_name)
                  }
                  this.thanksMessageState.set(
                    $localize`:@@landingPage.success.thanks.string:Thanks, ${this.givenNameState()} ${this.familyNameState()}!`
                  )

                  this.showSuccessElement()
                },
                error: (err) => {
                  this.showErrorElement(err)
                },
              })
            },
            error: (err) => {
              this.showErrorElement(err)
            },
          })
        } else {
          this.showErrorElement(response)
        }
      },
      error: (err) => {
        this.showErrorElement(err)
      },
    })
  }

  protected submitUserDenied(state: string) {
    this.landingPageService.submitUserResponse({ denied: true, state }).subscribe({
      next: () => {
        this.showDeniedElement()
      },
      error: (err) => {
        this.showErrorElement(err)
      },
    })
  }

  protected startTimer(seconds: number) {
    const timer = interval(100)
    timer.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((sec) => {
      this.progressbarValueState.set((sec * 100) / seconds)
      this.curSecState.set(sec)
    })
  }

  protected showConnectionExistsElement(): void {
    this.showDeniedState.set(false)
    this.showErrorState.set(false)
    this.showSuccessState.set(false)
    this.showConnectionExistsState.set(true)
    this.loadingState.set(false)
    this.showConnectionExistsDifferentUserState.set(false)
  }

  protected showConnectionExistsDifferentUserElement(): void {
    this.showDeniedState.set(false)
    this.showErrorState.set(false)
    this.showSuccessState.set(false)
    this.showConnectionExistsState.set(false)
    this.loadingState.set(false)
    this.showConnectionExistsDifferentUserState.set(true)
  }

  protected showErrorElement(err: any): void {
    console.error(err)
    this.showDeniedState.set(false)
    this.showErrorState.set(true)
    this.showSuccessState.set(false)
    this.loadingState.set(false)
    this.showConnectionExistsDifferentUserState.set(false)
  }

  protected showDeniedElement(): void {
    this.showDeniedState.set(true)
    this.showErrorState.set(false)
    this.showSuccessState.set(false)
    this.loadingState.set(false)
    this.showConnectionExistsDifferentUserState.set(false)
  }

  protected showSuccessElement(): void {
    this.showDeniedState.set(false)
    this.showErrorState.set(false)
    this.showSuccessState.set(true)
    this.loadingState.set(false)
    this.showConnectionExistsDifferentUserState.set(false)
  }
}
