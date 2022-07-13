import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRouteSnapshot, NavigationEnd, NavigationError } from '@angular/router';
import { BASE_URL, ORCID_BASE_URL } from 'app/app.constants';

import { JhiLanguageHelper } from 'app/core';

@Component({
  selector: 'jhi-main',
  templateUrl: './main.component.html',
  styles: [':host {display:flex; flex-direction: column; height:100%;}']
})
export class JhiMainComponent implements OnInit {
  PROD_BASE_URL = 'https://member-portal.orcid.org';
  baseUrl: string = BASE_URL;
  orcidBaseUrl: string = ORCID_BASE_URL;
  hideNav: Boolean = false;
  constructor(private jhiLanguageHelper: JhiLanguageHelper, private router: Router) {}

  private getPageTitle(routeSnapshot: ActivatedRouteSnapshot) {
    let title: string = routeSnapshot.data && routeSnapshot.data['pageTitle'] ? routeSnapshot.data['pageTitle'] : 'gatewayApp';
    if (routeSnapshot.firstChild) {
      title = this.getPageTitle(routeSnapshot.firstChild) || title;
    }
    return title;
  }

  ngOnInit() {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.jhiLanguageHelper.updateTitle(this.getPageTitle(this.router.routerState.snapshot.root));
        if (event.url.indexOf('landing-page') > -1) {
          this.hideNav = true;
        }
      }
      if (event instanceof NavigationError && event.error.status === 404) {
        this.router.navigate(['/404']);
      }
    });
  }
}
