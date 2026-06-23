/// <reference types="@angular/localize" />

import { bootstrapApplication } from '@angular/platform-browser'
import { enableProdMode, importProvidersFrom, provideZoneChangeDetection } from '@angular/core'

import { AppComponent } from './app/app.component'
import { AppModule } from './app/app.module'
import { environment } from './environments/environment'

if (environment.production) {
  enableProdMode()
}

bootstrapApplication(AppComponent, {
  providers: [importProvidersFrom(AppModule), provideZoneChangeDetection()],
})
  .catch((err) => console.error(err))
