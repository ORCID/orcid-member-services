import { ChangeDetectorRef, Component, HostListener, OnInit, inject } from '@angular/core'
import { Subscription } from 'rxjs'
import { AppError, ErrorAlert } from './model/error.model'
import { ErrorService } from './service/error.service'

@Component({
  selector: 'app-alert-error',
  templateUrl: './error-alert.component.html',
  standalone: false,
})
export class ErrorAlertComponent implements OnInit {
  private errorService = inject(ErrorService)
  private cdr = inject(ChangeDetectorRef)

  alerts: any[] = []
  sub: Subscription | undefined

  ngOnInit(): void {
    this.sub = this.errorService.on().subscribe((err: AppError) => {
      console.log(err)
      console.log('error message is ', err.message)

      const alert: ErrorAlert = {
        type: 'danger',
        msg: err.message,
        toast: false,
      }
      this.alerts.push(alert)
      this.cdr.detectChanges()
    })
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe()
  }

  @HostListener('document:keyup.escape')
  closeOldestAlert() {
    this.alerts.shift()
    this.cdr.detectChanges()
  }

  close(alertToRemove: any) {
    this.alerts = this.alerts.filter((alert: any) => alert !== alertToRemove)
    this.cdr.detectChanges()
  }
}
