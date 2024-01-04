import { Component } from '@angular/core'

@Component({
  selector: 'app-error-alert',
  templateUrl: './error-alert.component.html',
  styleUrls: ['./error-alert.component.scss'],
})
export class ErrorAlertComponent {
  constructor() {
    // subscribe to error handler
    // find 400 errors
    // look for translation key - if present somehow translate the fucker
    // set error fields in component for template to read
    // make it show

    // build list of alerts called alerts
    const newAlert = {
      type: 'danger',
      msg: message,
      params: data,
      toast: this.alertService.isToast(),
      scoped: true,
    }

    this.alerts.push(this.alertService.addAlert(newAlert, this.alerts))
  }
}
