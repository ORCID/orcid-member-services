import { ChangeDetectorRef, Component, ErrorHandler, Inject, OnInit } from '@angular/core'
import { ErrorService } from '../service/error.service'

@Component({
  selector: 'app-error-alert',
  templateUrl: './error-alert.component.html',
  styleUrls: ['./error-alert.component.scss'],
})
export class ErrorAlertComponent implements OnInit {
  alerts: any = []
  constructor(
    @Inject(ErrorHandler) private errorService: ErrorService,
    private cdr: ChangeDetectorRef
  ) {
    // subscribe to error handler
    // find 400 errors
    // look for translation key - if present somehow translate the fucker
    // set error fields in component for template to read
    // make it show
    // build list of alerts called alerts
    /* const newAlert = {
      type: 'danger',
      msg: message,
      params: data,
      toast: this.alertService.isToast(),
      scoped: true,
    }

     */
  }

  ngOnInit(): void {
    this.errorService.on().subscribe((err) => {
      const alerts = [...this.alerts]
      alerts.push({
        type: 'danger',
        msg: err.message,
        params: err.params,
        toast: false,
        dismiss: true,
      })
      this.alerts = alerts
      this.cdr.detectChanges()
    })
  }

  setClasses(alert: any) {
    return {
      'jhi-toast': alert.toast,
      [alert.position]: true,
    }
  }

  close(alertToRemove: any) {
    this.alerts = this.alerts.filter((alert: any) => alert !== alertToRemove)
    this.cdr.detectChanges()
    console.log(this.alerts)
  }
}
