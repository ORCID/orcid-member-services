import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';
import { ActivatedRoute } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { KEYUTIL, KJUR } from 'jsrsasign';
import { LandingPageService } from './landing-page.service';
import { IMSMember } from 'app/shared/model/member.model';
import { MSMemberService } from 'app/entities/member/member.service';
import { BASE_URL, ORCID_BASE_URL } from 'app/app.constants';

@Component({
  selector: 'jhi-landing-page',
  templateUrl: './landing-page.component.html'
})
export class LandingPageComponent implements OnInit {
  issuer: string = ORCID_BASE_URL;
  oauthBaseUrl: string = ORCID_BASE_URL + '/oauth/authorize';
  redirectUri: string = BASE_URL + '/landing-page';

  loading: Boolean = true;
  showConnectionExists: Boolean = false;
  showConnectionExistsDifferentUser: Boolean = false;
  showDenied: Boolean = false;
  showError: Boolean = false;
  showSuccess: Boolean = false;
  key: any;
  clientName: string;
  salesforceId: string;
  clientId: string;
  orcidId: string;
  oauthUrl: string;
  orcidRecord: any;
  signedInIdToken: any;
  givenName: string;
  familyName: string;
  progressbarValue = 100;
  curSec = 0;

  constructor(
    private eventManager: JhiEventManager,
    private landingPageService: LandingPageService,
    protected msMemberService: MSMemberService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const id_token_fragment = this.getFragmentParameterByName('id_token');
    const access_token_fragment = this.getFragmentParameterByName('access_token');
    const state_param = this.getQueryParameterByName('state');

    this.landingPageService.getOrcidConnectionRecord(state_param).subscribe(
      (result: HttpResponse<any>) => {
        this.orcidRecord = result.body;
        this.landingPageService.getMemberInfo(state_param).subscribe(
          (res: HttpResponse<IMSMember>) => {
            this.clientName = res.body.clientName;
            this.clientId = res.body.clientId;
            this.salesforceId = res.body.salesforceId;
            this.oauthUrl =
              this.oauthBaseUrl +
              '?response_type=token&redirect_uri=' +
              this.redirectUri +
              '&client_id=' +
              this.clientId +
              '&scope=/read-limited /activities/update /person/update openid&prompt=login&state=' +
              state_param;
            // Check if id token exists in URL (user just granted permission)
            if (id_token_fragment != null && id_token_fragment !== '') {
              this.checkSubmitToken(id_token_fragment, state_param, access_token_fragment);
            } else {
              const error = this.getFragmentParameterByName('error');
              // Check if user denied permission
              if (error != null && error !== '') {
                if (error === 'access_denied') {
                  this.submitUserDenied(state_param);
                } else {
                  this.showErrorElement();
                }
              } else {
                window.location.replace(this.oauthUrl);
              }
            }

            this.startTimer(600);
          },
          (res: HttpErrorResponse) => {
            console.log('error');
          }
        );
      },
      (res: HttpErrorResponse) => {
        console.log('error');
      }
    );
  }

  getFragmentParameterByName(name: string): string {
    name = name.replace(/[\[]/, '\\[').replace(/[\]]/, '\\]');
    const regex = new RegExp('[\\#&]' + name + '=([^&#]*)'),
      results = regex.exec(window.location.hash);
    return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '));
  }

  getQueryParameterByName(name: string): string {
    name = name.replace(/[\[\]]/g, '\\$&');
    const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
      results = regex.exec(window.location.href);
    if (!results) {
      return null;
    }
    if (!results[2]) {
      return '';
    }
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
  }

  checkSubmitToken(id_token: string, state: string, access_token: string) {
    this.landingPageService.getPublicKey().subscribe(
      res => {
        const pubKey = KEYUTIL.getKey(res.keys[0]);
        const response = KJUR.jws.JWS.verifyJWT(id_token, pubKey, {
          alg: ['RS256'],
          iss: [this.issuer],
          aud: this.clientId,
          gracePeriod: 15 * 60 // 15 mins skew allowed
        });
        if (response === true) {
          // check if existing token belongs to a different user

          this.landingPageService.submitUserResponse({ id_token, state, salesforce_id: this.salesforceId }).subscribe(
            res => {
              const data = res;
              if (data) {
                if (data.isDifferentUser) {
                  this.showConnectionExistsDifferentUserElement();
                  return;
                }
                if (data.isSameUserThatAlreadyGranted) {
                  this.showConnectionExistsElement();
                  return;
                }
              }
              this.landingPageService.getUserInfo(access_token).subscribe(
                (result: HttpResponse<any>) => {
                  this.signedInIdToken = result;
                  this.givenName = '';
                  if (this.signedInIdToken.given_name) {
                    this.givenName = this.signedInIdToken.given_name;
                  }
                  this.familyName = '';
                  if (this.signedInIdToken.family_name) {
                    this.familyName = this.signedInIdToken.family_name;
                  }

                  this.showSuccessElement();
                },
                () => {
                  this.showErrorElement();
                }
              );
            },
            () => {
              this.showErrorElement();
            }
          );
        } else {
          this.showErrorElement();
        }
      },
      () => {
        this.showErrorElement();
      }
    );
  }

  submitIdTokenData(id_token: string, state: string, access_token: string) {
    this.landingPageService.submitUserResponse({ id_token, state }).subscribe(
      () => {
        this.landingPageService.getUserInfo(access_token).subscribe(
          (res: HttpResponse<any>) => {
            this.signedInIdToken = res;
            this.showSuccessElement();
          },
          () => {
            this.showErrorElement();
          }
        );
      },
      () => {
        this.showErrorElement();
      }
    );
  }

  submitUserDenied(state: string) {
    this.landingPageService.submitUserResponse({ denied: true, state }).subscribe(
      () => {
        this.showDeniedElement();
      },
      () => {
        this.showErrorElement();
      }
    );
  }

  startTimer(seconds: number) {
    const timer = interval(100);
    const sub = timer.subscribe(sec => {
      this.progressbarValue = (sec * 100) / seconds;
      this.curSec = sec;
      if (this.curSec === seconds) {
        sub.unsubscribe();
      }
    });
  }

  showConnectionExistsElement(): void {
    this.showDenied = false;
    this.showError = false;
    this.showSuccess = false;
    this.showConnectionExists = true;
    this.loading = false;
    this.showConnectionExistsDifferentUser = false;
  }

  showConnectionExistsDifferentUserElement(): void {
    this.showDenied = false;
    this.showError = false;
    this.showSuccess = false;
    this.showConnectionExists = false;
    this.loading = false;
    this.showConnectionExistsDifferentUser = true;
  }

  showErrorElement(): void {
    this.showDenied = false;
    this.showError = true;
    this.showSuccess = false;
    this.loading = false;
    this.showConnectionExistsDifferentUser = false;
  }

  showDeniedElement(): void {
    this.showDenied = true;
    this.showError = false;
    this.showSuccess = false;
    this.loading = false;
    this.showConnectionExistsDifferentUser = false;
  }

  showSuccessElement(): void {
    this.showDenied = false;
    this.showError = false;
    this.showSuccess = true;
    this.loading = false;
    this.showConnectionExistsDifferentUser = false;
  }
}
