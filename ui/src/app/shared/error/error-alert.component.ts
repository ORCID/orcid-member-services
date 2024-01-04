import { Component } from '@angular/core'
import { ErrorService } from '../service/error.service'
import { Subscription } from 'rxjs/internal/Subscription'
import { ErrorAlert } from '../model/error-alert'

@Component({
  selector: 'app-error-alert',
  templateUrl: './error-alert.component.html',
  styleUrls: ['./error-alert.component.scss'],
})
export class ErrorAlertComponent {
  sub: Subscription | undefined

  alerts: any[]

  constructor(private errorService: ErrorService) {
    // subscribe to error handler
    // find 400 errors
    // look for translation key - if present somehow translate the fucker
    // set error fields in component for template to read
    // make it show

    // build list of alerts called alerts

    this.alerts = []

    this.sub = this.errorService.on().subscribe((e) => {
      const alert: ErrorAlert = {
        type: 'danger',
        msg: message,
        params: data,
        toast: this.alertService.isToast(),
        scoped: true,
      }

      this.alerts.push(alert)
    })
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe()
  }
}
