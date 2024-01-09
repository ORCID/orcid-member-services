import { ChangeDetectorRef, Component, ErrorHandler, Inject, OnInit } from '@angular/core'
import { ErrorService } from '../service/error.service'
import { Subscription } from 'rxjs'
import { ErrorAlert } from '../model/error-alert'
import { AppError } from '../model/error.model'

@Component({
  selector: 'app-error-alert',
  templateUrl: './error-alert.component.html',
  styleUrls: ['./error-alert.component.scss'],
})
export class ErrorAlertComponent implements OnInit {
  alerts: any[] = []
  sub: Subscription | undefined

  constructor(
    @Inject(ErrorHandler) private errorService: ErrorService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.sub = this.errorService.on().subscribe((err: AppError) => {
      const alerts = [...this.alerts]
      const alert: ErrorAlert = {
        type: 'danger',
        msg: err.message,
        toast: false,
      }
      this.alerts.push(alert)
      this.alerts.push(alerts)
      this.cdr.detectChanges()
    })
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe()
  }

  close(alertToRemove: any) {
    this.alerts = this.alerts.filter((alert: any) => alert !== alertToRemove)
    this.cdr.detectChanges()
    console.log(this.alerts)
  }
}
