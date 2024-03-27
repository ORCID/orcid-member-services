import { Component, OnInit } from '@angular/core'
import { HttpErrorResponse, HttpResponse } from '@angular/common/http'
import { interval } from 'rxjs'
import { KEYUTIL, KJUR, RSAKey } from 'jsrsasign'
import { LandingPageService } from './landing-page.service'
import { MemberService } from '../member/service/member.service'
import { IMember } from '../member/model/member.model'
import { ORCID_BASE_URL } from '../app.constants'

@Component({
  selector: 'app-landing-page',
  templateUrl: './landing-page.component.html',
})
export class LandingPageComponent implements OnInit {
  issuer: string = ORCID_BASE_URL
  oauthBaseUrl: string = ORCID_BASE_URL + '/oauth/authorize'
  redirectUri: string = '/landing-page'

  loading: Boolean = true
  showConnectionExists: Boolean = false
  showConnectionExistsDifferentUser: Boolean = false
  showDenied: Boolean = false
  showError: Boolean = false
  showSuccess: Boolean = false
  key: any
  clientName: string | undefined
  salesforceId: string | undefined
  clientId: string | undefined
  orcidId: string | undefined
  oauthUrl: string | undefined
  orcidRecord: any
  signedInIdToken: any
  givenName: string | undefined
  familyName: string | undefined
  progressbarValue = 100
  curSec = 0
  incorrectDataMessage = ''
  linkAlreadyUsedMessage = ''
  allowToUpdateRecordMessage = ''
  successfullyGrantedMessage = ''
  thanksMessage = ''

  constructor(
    private landingPageService: LandingPageService,
    protected memberService: MemberService
  ) {}

  ngOnInit() {
    const id_token_fragment = this.getFragmentParameterByName('id_token')
    const access_token_fragment = this.getFragmentParameterByName('access_token')
    const state_param = this.getQueryParameterByName('state')

    if (state_param) {
      this.landingPageService.getOrcidConnectionRecord(state_param).subscribe({
        next: (result: HttpResponse<any>) => {
          this.orcidRecord = result.body
          this.landingPageService.getMemberInfo(state_param).subscribe({
            next: (res: IMember) => {
              this.clientName = res.clientName
              this.clientId = res.clientId
              this.salesforceId = res.salesforceId
              this.oauthUrl =
                this.oauthBaseUrl +
                '?response_type=token&redirect_uri=' +
                this.redirectUri +
                '&client_id=' +
                this.clientId +
                '&scope=/read-limited /activities/update /person/update openid&prompt=login&state=' +
                state_param

              this.incorrectDataMessage = $localize`:@@landingPage.success.ifYouFind:If you find that data added to your ORCID record is incorrect, please contact ${this.clientName}`
              this.linkAlreadyUsedMessage = $localize`:@@landingPage.connectionExists.differentUser.string:This authorization link has already been used. Please contact ${this.clientName} for a new authorization link.`
              this.allowToUpdateRecordMessage = $localize`:@@landingPage.denied.grantAccess.string:Allow ${this.clientName} to update my ORCID record.`
              this.successfullyGrantedMessage = $localize`:@@landingPage.success.youHaveSuccessfully.string:You have successfully granted ${this.clientName} permission to update your ORCID record, and your record has been updated with affiliation information.`

              // Check if id token exists in URL (user just granted permission)
              if (id_token_fragment != null && id_token_fragment !== '') {
                this.checkSubmitToken(id_token_fragment, state_param, access_token_fragment)
              } else {
                const error = this.getFragmentParameterByName('error')
                // Check if user denied permission
                if (error != null && error !== '') {
                  if (error === 'access_denied') {
                    this.submitUserDenied(state_param)
                  } else {
                    this.showErrorElement()
                  }
                } else {
                  window.location.replace(this.oauthUrl)
                }
              }

              this.startTimer(600)
            },
            error: (res: HttpErrorResponse) => {
              console.log('error')
            },
          })
        },
        error: (res: HttpErrorResponse) => {
          console.log('error')
        },
      })
    }
  }

  getFragmentParameterByName(name: string): string {
    name = name.replace(/[\[]/, '\\[').replace(/[\]]/, '\\]')
    const regex = new RegExp('[\\#&]' + name + '=([^&#]*)'),
      results = regex.exec(window.location.hash)
    return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '))
  }

  getQueryParameterByName(name: string): string | null {
    name = name.replace(/[\[\]]/g, '\\$&')
    const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
      results = regex.exec(window.location.href)
    if (!results) {
      return null
    }
    if (!results[2]) {
      return ''
    }
    return decodeURIComponent(results[2].replace(/\+/g, ' '))
  }

  checkSubmitToken(id_token: string, state: string, access_token: string) {
    this.landingPageService.getPublicKey().subscribe(
      (res) => {
        const pubKey = KEYUTIL.getKey(res.keys[0]) as RSAKey
        const response = KJUR.jws.JWS.verifyJWT(id_token, pubKey, {
          alg: ['RS256'],
          iss: [this.issuer],
          aud: [this.clientId || ''],
          gracePeriod: 15 * 60, // 15 mins skew allowed
        })
        if (response === true) {
          // check if existing token belongs to a different user

          this.landingPageService.submitUserResponse({ id_token, state, salesforce_id: this.salesforceId }).subscribe({
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
                  this.signedInIdToken = result
                  this.givenName = ''
                  if (this.signedInIdToken.given_name) {
                    this.givenName = this.signedInIdToken.given_name
                  }
                  this.familyName = ''
                  if (this.signedInIdToken.family_name) {
                    this.familyName = this.signedInIdToken.family_name
                  }
                  this.thanksMessage = $localize`:@@landingPage.success.thanks.string:Thanks, ${this.givenName} ${this.familyName}!`

                  this.showSuccessElement()
                },
                error: () => {
                  this.showErrorElement()
                },
              })
            },
            error: () => {
              this.showErrorElement()
            },
          })
        } else {
          this.showErrorElement()
        }
      },
      () => {
        this.showErrorElement()
      }
    )
  }

  submitIdTokenData(id_token: string, state: string, access_token: string) {
    this.landingPageService.submitUserResponse({ id_token, state }).subscribe({
      next: () => {
        this.landingPageService.getUserInfo(access_token).subscribe({
          next: (res: HttpResponse<any>) => {
            this.signedInIdToken = res
            this.showSuccessElement()
          },
          error: () => {
            this.showErrorElement()
          },
        })
      },
      error: () => {
        this.showErrorElement()
      },
    })
  }

  submitUserDenied(state: string) {
    this.landingPageService.submitUserResponse({ denied: true, state }).subscribe(
      () => {
        this.showDeniedElement()
      },
      () => {
        this.showErrorElement()
      }
    )
  }

  startTimer(seconds: number) {
    const timer = interval(100)
    const sub = timer.subscribe((sec) => {
      this.progressbarValue = (sec * 100) / seconds
      this.curSec = sec
      if (this.curSec === seconds) {
        sub.unsubscribe()
      }
    })
  }

  showConnectionExistsElement(): void {
    this.showDenied = false
    this.showError = false
    this.showSuccess = false
    this.showConnectionExists = true
    this.loading = false
    this.showConnectionExistsDifferentUser = false
  }

  showConnectionExistsDifferentUserElement(): void {
    this.showDenied = false
    this.showError = false
    this.showSuccess = false
    this.showConnectionExists = false
    this.loading = false
    this.showConnectionExistsDifferentUser = true
  }

  showErrorElement(): void {
    this.showDenied = false
    this.showError = true
    this.showSuccess = false
    this.loading = false
    this.showConnectionExistsDifferentUser = false
  }

  showDeniedElement(): void {
    this.showDenied = true
    this.showError = false
    this.showSuccess = false
    this.loading = false
    this.showConnectionExistsDifferentUser = false
  }

  showSuccessElement(): void {
    this.showDenied = false
    this.showError = false
    this.showSuccess = true
    this.loading = false
    this.showConnectionExistsDifferentUser = false
  }
}