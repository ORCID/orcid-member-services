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
    console.log('before alertservice on')

    this.sub = this.alertService.on().subscribe((alert: AppAlert) => {
      console.log('test')

      console.log(alert.msg)
      const messageKey = '@@' + alert.msg
      //this.message = $localize`${messageKey}`
      const id = alert.msg
      this.message = $localize(<any>{ '0': `:@@${id}:${id}`, raw: [':'] })

      this.alerts.push(alert)
      this.cdr.detectChanges()
    })
    console.log('after alertservice on')
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
