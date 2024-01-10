import { ChangeDetectorRef, Component, ErrorHandler, HostListener, Inject, OnInit } from '@angular/core'
import { ErrorService } from './service/error.service'
import { Subscription } from 'rxjs'
import { AppError, ErrorAlert } from './model/error.model'

@Component({
  selector: 'app-error-alert',
  templateUrl: './error-alert.component.html'
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

  @HostListener('document:keyup.escape', ['$event'])
  closeOldestAlert() {
    this.alerts.shift()
    this.cdr.detectChanges()
  }

  close(alertToRemove: any) {
    this.alerts = this.alerts.filter((alert: any) => alert !== alertToRemove)
    this.cdr.detectChanges()
  }
}
