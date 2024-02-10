import { ChangeDetectorRef, Component, ErrorHandler, HostListener, Inject, OnInit } from '@angular/core'
import { Subscription, filter } from 'rxjs'
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
    this.sub = this.alertService.on().subscribe((alerts: AppAlert[]) => {
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
    this.alerts.shift()
    this.cdr.detectChanges()
  }

  close(alertToRemove: any) {
    this.alertService.clear(alertToRemove)
    this.cdr.detectChanges()
  }
}
