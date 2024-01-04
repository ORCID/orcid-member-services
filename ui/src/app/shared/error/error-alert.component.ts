import { Component } from '@angular/core'
import { ErrorService } from '../service/error.service'
import { Subscription } from 'rxjs/internal/Subscription'
import { ErrorAlert } from '../model/error-alert'
import { AppError } from '../model/error.model'

@Component({
  selector: 'app-error-alert',
  templateUrl: './error-alert.component.html',
  styleUrls: ['./error-alert.component.scss'],
})
export class ErrorAlertComponent {
  sub: Subscription | undefined

  alerts: any[]

  constructor(private errorService: ErrorService) {
    // look for translation key - if present somehow translate the fucker
    // set error fields in component for template to read

    this.alerts = []

    this.sub = this.errorService.on().subscribe((e: AppError) => {
      if (e.statusCode == 404) {
        if (e.i18nKey) {
          // set key or actual translated message in below alert object?
        }

        const alert: ErrorAlert = {
          type: 'danger',
          msg: e.message,
          params: '',
          toast: false, // previously this.alertService.isToast(),
          scoped: true,
        }

        this.alerts.push(alert)
      }
    })

    // make it show
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe()
  }
}
