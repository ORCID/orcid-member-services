import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';
import { ActivatedRoute } from '@angular/router';
import { KEYUTIL, KJUR } from 'jsrsasign';
import { LandingPageService } from './landing-page.service';
import { IMSUser } from 'app/shared/model/MSUserService/ms-user.model';
import { MSUserService } from 'app/entities/MSUserService/ms-users/ms-user.service';
import { IMSMember } from 'app/shared/model/MSUserService/ms-member.model';
import { MSMemberService } from 'app/entities/MSUserService/ms-members/ms-member.service';

@Component({
  selector: 'jhi-landing-page',
  templateUrl: './landing-page.component.html'
})
export class LandingPageComponent implements OnInit {

  const sandboxIssuer: string = 'https://sandbox.orcid.org';
  const sandboxOauthUrl: string = 'https://sandbox.orcid.org/oauth/authorize';
  // TODO: should be configurable
  const redirectUri: string = 'http://localhost:8080/landing-page';
  const sandboxKey = {'kty': 'RSA', 'e': 'AQAB', 'use': 'sig', 'kid': 'sandbox-orcid-org-3hpgosl3b6lapenh1ewsgdob3fawepoj', 'n': 'pl-jp-kTAGf6BZUrWIYUJTvqqMVd4iAnoLS6vve-KNV0q8TxKvMre7oi9IulDcqTuJ1alHrZAIVlgrgFn88MKirZuTqHG6LCtEsr7qGD9XyVcz64oXrb9vx4FO9tLNQxvdnIWCIwyPAYWtPMHMSSD5oEVUtVL_5IaxfCJvU-FchdHiwfxvXMWmA-i3mcEEe9zggag2vUPPIqUwbPVUFNj2hE7UsZbasuIToEMFRZqSB6juc9zv6PEUueQ5hAJCEylTkzMwyBMibrt04TmtZk2w9DfKJR91555s2ZMstX4G_su1_FqQ6p9vgcuLQ6tCtrW77tta-Rw7McF_tyPmvnhQ'};

  showConnectionExists: Boolean = false;
  showDenied: Boolean = false;
  showError: Boolean = false;
  showSuccess: Boolean = false;
  issuer: string;
  key: any;
  oauthUrl: string;
  clientName: string;
  clientId: string;
  orcidId: string;
  orcidRecord: any;

  constructor(
    private eventManager: JhiEventManager,
    private landingPageService: LandingPageService,
    protected msUserService: MSUserService,
    protected msMemberService: MSMemberService,
    private route: ActivatedRoute
  ) {
    // TODO: need to make this switch from sandbox to prod
    this.issuer = this.sandboxIssuer;
    this.key = this.sandboxKey;
  }

  ngOnInit() {
    let id_token_fragment = this.getFragmentParameterByName('id_token');
    let state_param = this.getQueryParameterByName('state');

    this.landingPageService.getOrcidConnectionRecord(state_param)
      .subscribe(
        (res: HttpResponse<any>) => {
          this.orcidRecord = res;
          console.log(res.body);
          this.landingPageService.getMemberInfo(state_param).subscribe(
            (res: HttpResponse<IMSMember>) => {
              this.clientName = res.body.clientName;
              this.clientId = res.body.clientId;
              this.oauthUrl = this.sandboxOauthUrl + '?response_type=token&redirect_uri=' + this.redirectUri + '&client_id=' + this.clientId + '&scope=/activities/update openid&state=' + state_param;

              //Check if id token already exists in DB (user previously granted permission)
              if(this.orcidRecord.idToken != null && this.orcidRecord.idToken != ''){
                this.showConnectionExistsElement();
              } else {
                //Check if id token exists in URL (user just granted permission)
                if (id_token_fragment != null && id_token_fragment != '') {
                  if (this.checkSig(id_token_fragment)) {
                    this.submitIdTokenData(id_token_fragment, state_param);
                  } else {
                    this.showErrorElement();
                  }
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

  checkSig(id_token: string) {
    let pubKey = KEYUTIL.getKey(this.key);
    return KJUR.jws.JWS.verifyJWT(id_token, pubKey, {
      alg: ['RS256'], iss: [this.issuer], aud: this.clientId, gracePeriod: 15 * 60 // 15 mins skew allowed
    });
  }

  submitIdTokenData(id_token: string, state: string) {
    this.landingPageService.submitUserResponse({ 'id_token': id_token, 'state': state}).subscribe(
      () => {
        this.showSuccessElement();
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

  buildOauthUrl(state_param: string) {
    this.landingPageService.getMemberInfo(state_param).subscribe(
      (res: HttpResponse<IMSMember>) => {
        this.clientName = res.body.clientName;
        this.clientId = res.body.clientId;
        this.oauthUrl = this.sandboxOauthUrl + '?response_type=token&redirect_uri=' + this.redirectUri + '&client_id=' + this.clientId + '&scope=/activities/update openid&state=' + state_param;;
      },
      () => {
        console.log("error")
      }
    );
  }

  showConnectionExistsElement(): void {
    this.showDenied = false;
    this.showError = false;
    this.showSuccess = false;
    this.showConnectionExists = true;
  }

  showErrorElement(): void {
    this.showDenied = false;
    this.showError = true;
    this.showSuccess = false;
  }

  showDeniedElement(): void {
    this.showDenied = true;
    this.showError = false;
    this.showSuccess = false;
  }

  showSuccessElement(): void {
    this.showDenied = false;
    this.showError = false;
    this.showSuccess = true;
  }
}
