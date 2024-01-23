import { ChangeDetectorRef, Component, ErrorHandler, HostListener, Inject, OnInit } from '@angular/core'
import { Subscription } from 'rxjs'
import { AlertService } from '../service/alert.service'
import { AppAlert } from './model/alert.model'

@Component({
  selector: 'app-alert',
  templateUrl: './alert.component.html',
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
    this.sub = this.alertService.on().subscribe((alert: AppAlert) => {
      this.alerts.push(alert)
      this.cdr.detectChanges()
      setTimeout(() => this.close(alert), 5000)
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
