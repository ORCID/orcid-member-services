import { Routes } from '@angular/router'
import { LandingPageComponent } from './landing-page.component'

export const LANDING_PAGE_ROUTE: Routes = [
  {
    path: '',
    component: LandingPageComponent,
    data: {
      authorities: [],
      pageTitle: 'landingPage.title.string',
    },
  },
]
