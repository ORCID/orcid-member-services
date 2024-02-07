import { Route } from '@angular/router'

import { ActivationComponent } from './activation.component'

export const activationRoute: Route = {
  path: 'activate',
  component: ActivationComponent,
  data: {
    authorities: [],
    pageTitle: 'activate.title.string',
  },
}
