import { ChangeDetectorRef, Component, ErrorHandler, HostListener, Inject, OnInit } from '@angular/core'
import { Subscription, filter, map } from 'rxjs'
import { AlertService } from '../service/alert.service'
import { AppAlert } from './model/alert.model'
import { AlertType } from 'src/app/app.constants'

@Component({
  selector: 'app-alert-toast',
  templateUrl: './alert-toast.component.html',
})
export class AlertComponent implements OnInit {
  alerts: any[] = []
  sub: Subscription | undefined
  message: any

  constructor(
    private alertService: AlertService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.sub = this.alertService
      .on()
      .pipe(map((alerts) => alerts?.filter((alert) => alert.type === AlertType.TOAST) as AppAlert[]))
      .subscribe((alerts: AppAlert[]) => {
        this.alerts = alerts
        this.cdr.detectChanges()
      })
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe()
  }

  @HostListener('document:keyup.escape', ['$event'])
  @HostListener('document:keyup.enter', ['$event'])
  closeOldestAlert() {
    this.alertService.clear(this.alerts[0])
    this.cdr.detectChanges()
  }

  close(alertToRemove: any) {
    this.alertService.clear(alertToRemove)
    this.cdr.detectChanges()
  }
}
