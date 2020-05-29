import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';
import { ActivatedRoute } from '@angular/router';
import { KEYUTIL, KJUR } from 'jsrsasign';
import { LandingPageService } from './landing-page.service';
import { IMSMember } from 'app/shared/model/MSUserService/ms-member.model';
import { MSMemberService } from 'app/entities/MSUserService/ms-members/ms-member.service';
import { BASE_URL, ORCID_BASE_URL } from 'app/app.constants';

@Component({
  selector: 'jhi-landing-page',
  templateUrl: './landing-page.component.html'
})
export class LandingPageComponent implements OnInit {

  const issuer: string = ORCID_BASE_URL;
  const oauthBaseUrl: string = ORCID_BASE_URL + '/oauth/authorize';
  const redirectUri: string = BASE_URL + '/landing-page';

  loading: Boolean = true;
  showConnectionExists: Boolean = false;
  showDenied: Boolean = false;
  showError: Boolean = false;
  showSuccess: Boolean = false;
  key: any;
  clientName: string;
  clientId: string;
  orcidId: string;
  oauthUrl: string;
  orcidRecord: any;
  signedInIdToken: any;

  constructor(
    private eventManager: JhiEventManager,
    private landingPageService: LandingPageService,
    protected msMemberService: MSMemberService,
    private route: ActivatedRoute
  ) {

  }

  ngOnInit() {
    let id_token_fragment = this.getFragmentParameterByName('id_token');
    let access_token_fragment = this.getFragmentParameterByName('access_token');
    let state_param = this.getQueryParameterByName('state');

    this.landingPageService.getOrcidConnectionRecord(state_param)
      .subscribe(
        (res: HttpResponse<any>) => {
          this.orcidRecord = res.body;
          this.landingPageService.getMemberInfo(state_param).subscribe(
            (res: HttpResponse<IMSMember>) => {
              this.clientName = res.body.clientName;
              this.clientId = res.body.clientId;
              this.oauthUrl = this.oauthBaseUrl + '?response_type=token&redirect_uri=' + this.redirectUri + '&client_id=' + this.clientId + '&scope=/activities/update openid&state=' + state_param;
              //Check if id token already exists in DB (user previously granted permission)
              if(this.orcidRecord.idToken != null && this.orcidRecord.idToken != ''){
                this.showConnectionExistsElement();
              } else {
                //Check if id token exists in URL (user just granted permission)
                if (id_token_fragment != null && id_token_fragment != '') {
                    this.checkSubmitToken(id_token_fragment, state_param, access_token_fragment);
                } else {
                  let error = this.getFragmentParameterByName('error');
                  //Check if user denied permission
                  if (error != null && error != '') {
                    if (error === 'access_denied') {
                      this.submitUserDenied(state_param);
                    } else {
                      this.showErrorElement();
                    }
                  } else {
                    window.location.replace(this.oauthUrl);
                  }
                }

              }
            },
            (res: HttpErrorResponse) => {
              console.log("error")
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
      let regex = new RegExp('[\\#&]' + name + '=([^&#]*)'),
          results = regex.exec(window.location.hash);
      return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '));
  }

  getQueryParameterByName(name: string): string {
      name = name.replace(/[\[\]]/g, '\\$&');
      let regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
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
      (res) => {
        let pubKey = KEYUTIL.getKey(res.keys[0]);
        let response = KJUR.jws.JWS.verifyJWT(id_token, pubKey, {
          alg: ['RS256'], iss: [this.issuer], aud: this.clientId, gracePeriod: 15 * 60 // 15 mins skew allowed
        });
        if(response === true){
          this.landingPageService.submitUserResponse({ 'id_token': id_token, 'state': state}).subscribe(
            () => {
              this.landingPageService.getUserInfo(access_token).subscribe(
                (res: HttpResponse<any>) => {
                  this.signedInIdToken = res;
                  this.showSuccessElement();
                },
                () => {
                  this.showErrorElement();
                });
            },
            () => {
              this.showErrorElement();
            });
        } else {
          this.showErrorElement();
        }
      },
      () => {
        this.showErrorElement();
      });

  }

  submitIdTokenData(id_token: string, state: string, access_token: string) {
    this.landingPageService.submitUserResponse({ 'id_token': id_token, 'state': state}).subscribe(
      () => {
        this.landingPageService.getUserInfo(access_token).subscribe(
          (res: HttpResponse<any>) => {
            this.signedInIdToken = res;
            this.showSuccessElement();
          },
          () => {
            this.showErrorElement();
          });
      },
      () => {
        this.showErrorElement();
      });
  }

  submitUserDenied(state: string) {
    this.landingPageService.submitUserResponse({ 'denied': true, 'state': state}).subscribe(
      () => {
        this.showDeniedElement();

      },
      () => {
        this.showErrorElement();
      }
    );
  }

  showConnectionExistsElement(): void {
    this.showDenied = false;
    this.showError = false;
    this.showSuccess = false;
    this.showConnectionExists = true;
    this.loading = false;
  }

  showErrorElement(): void {
    this.showDenied = false;
    this.showError = true;
    this.showSuccess = false;
    this.loading = false;
  }

  showDeniedElement(): void {
    this.showDenied = true;
    this.showError = false;
    this.showSuccess = false;
    this.loading = false;
  }

  showSuccessElement(): void {
    this.showDenied = false;
    this.showError = false;
    this.showSuccess = true;
    this.loading = false;
  }
}
