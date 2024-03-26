import { Route } from '@angular/router'
import { LandingPageComponent } from './landing-page.component'

export const LANDING_PAGE_ROUTE: Route = {
  path: 'landing-page',
  component: LandingPageComponent,
  data: {
    authorities: [],
    pageTitle: 'landingPage.title.string',
  },
}
