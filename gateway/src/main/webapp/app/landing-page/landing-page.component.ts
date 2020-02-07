import { Component, OnInit } from '@angular/core';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';
import { ActivatedRoute } from '@angular/router';
import { KEYUTIL, KJUR } from 'jsrsasign';
import { LandingPageService } from './landing-page.service';
import { AccountService, Account } from 'app/core';

@Component({
  selector: 'jhi-landing-page',
  templateUrl: './landing-page.component.html'
})
export class LandingPageComponent implements OnInit {

  const sandboxIssuer: String = 'https://sandbox.orcid.org';
  const sandboxOauthUrl: String = 'https://sandbox.orcid.org/oauth/authorize';
  const clientId: String = 'APP-99MR3GU01PBQK8FP';
  // TODO: should be configurable
  const redirectUri: String = 'http://localhost:8080/landing-page';
  const sandboxKey = {'kty': 'RSA', 'e': 'AQAB', 'use': 'sig', 'kid': 'sandbox-orcid-org-3hpgosl3b6lapenh1ewsgdob3fawepoj', 'n': 'pl-jp-kTAGf6BZUrWIYUJTvqqMVd4iAnoLS6vve-KNV0q8TxKvMre7oi9IulDcqTuJ1alHrZAIVlgrgFn88MKirZuTqHG6LCtEsr7qGD9XyVcz64oXrb9vx4FO9tLNQxvdnIWCIwyPAYWtPMHMSSD5oEVUtVL_5IaxfCJvU-FchdHiwfxvXMWmA-i3mcEEe9zggag2vUPPIqUwbPVUFNj2hE7UsZbasuIToEMFRZqSB6juc9zv6PEUueQ5hAJCEylTkzMwyBMibrt04TmtZk2w9DfKJR91555s2ZMstX4G_su1_FqQ6p9vgcuLQ6tCtrW77tta-Rw7McF_tyPmvnhQ'};

  showOauth: Boolean = false;
  showDenied: Boolean = false;
  showError: Boolean = true;
  showSuccess: Boolean = false;
  issuer: String;
  key: any;
  oauthUrl: String;
  signedInIdToken: any;

  constructor(private eventManager: JhiEventManager, private landingPageService: LandingPageService, private route: ActivatedRoute) {
    // TODO: need to make this switch from sandbox to prod
    this.issuer = this.sandboxIssuer;
    this.key = this.sandboxKey;
    this.oauthUrl = this.sandboxOauthUrl + '?response_type=token&redirect_uri=' + this.redirectUri + '&client_id=' + this.clientId + '&scope=openid%20/activities/update';
  }

  ngOnInit() {
    let id_token = this.getFragmentParameterByName('id_token');
    let state_param = this.getFragmentParameterByName('state');
    
    if (id_token != null && id_token != '') {
      if (this.checkSig(id_token)) {
        this.signedInIdToken = JSON.parse(KJUR.jws.JWS.parse(id_token).payloadPP);
        this.submitIdTokenData(this.signedInIdToken, state_param);
      } else {
        this.showErrorElement();
      }
    } else {
      this.showOauthButton();
      state_param = this.getQueryParameterByName('state');
      let error = this.getFragmentParameterByName('error');
      if (error != null && error != '') {
        if (error === 'access_denied') {
          this.submitUserDenied(state_param);
          this.showErrorElement();
        } else {
          this.showErrorElement();
        }
      } else {
        if (state_param != null) {
          this.oauthUrl = this.oauthUrl + '&state=' + state_param;
        }
        this.showOauthButton();
      }
    }
  }

  getFragmentParameterByName(name: String): String {
      name = name.replace(/[\[]/, '\\[').replace(/[\]]/, '\\]');
      let regex = new RegExp('[\\#&]' + name + '=([^&#]*)'),
          results = regex.exec(window.location.hash);
      return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '));
  }

  getQueryParameterByName(name: String): String {
      name = name.replace(/[\[\]]/g, '\\$&');
      let regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
          results = regex.exec(window.location.href);
      if (!results) return null;
      if (!results[2]) return '';
      return decodeURIComponent(results[2].replace(/\+/g, ' '));
  }
  
  checkSig(id_token: String) {
    let pubKey = KEYUTIL.getKey(this.key);
    return KJUR.jws.JWS.verifyJWT(id_token, pubKey, {
      alg: ['RS256'], iss: [this.issuer], aud: this.clientId, gracePeriod: 15 * 60 // 15 mins skew allowed
    });
  }

  submitIdTokenData(id_token: String, state: String) {
    this.landingPageService.submitUserResponse({ 'id_token': id_token, 'state': state}).subscribe(
      () => {
        this.showSuccessElement();
      },
      () => {
        this.showErrorElement();
      });
  }

  submitUserDenied(state: String) {
    this.landingPageService.submitUserResponse({ 'denied': true, 'state': state}).subscribe(
      () => {
        this.showDeniedElement();
      },
      () => {
        this.showErrorElement();
      }
    );
  }

  showErrorElement(): void {
    this.showOauth = false;
    this.showDenied = false;
    this.showError = true;
    this.showSuccess = false;
  }

  showDeniedElement(): void {
    this.showOauth = false;
    this.showDenied = true;
    this.showError = false;
    this.showSuccess = false;
  }

  showSuccessElement(): void {
    this.showOauth = false;
    this.showDenied = false;
    this.showError = false;
    this.showSuccess = true;
  }

  showOauthButton(): void {
    this.showOauth = true;
    this.showDenied = false;
    this.showError = false;
    this.showSuccess = false;
  }
}
