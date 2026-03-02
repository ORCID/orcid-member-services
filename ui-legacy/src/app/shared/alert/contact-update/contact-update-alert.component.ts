import { ChangeDetectorRef, Component, HostListener } from '@angular/core'
import { Subscription, map } from 'rxjs'
import { AlertService } from '../../service/alert.service'
import { AlertType } from 'src/app/app.constants'
import { AppAlert } from '../model/alert.model'

@Component({
  selector: 'app-contact-update-alert',
  templateUrl: './contact-update-alert.component.html',
  styleUrls: ['../overlay-modal.scss'],
})
export class ContactUpdateAlertComponent {
  alerts: AppAlert[] | undefined
  sub: Subscription | undefined
  message: any

  constructor(
    private alertService: AlertService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.sub = this.alertService
      .on()
      .pipe(map((alerts) => alerts?.filter((alert) => alert.type === AlertType.CONTACT_UPDATED)))
      .subscribe((alerts: AppAlert[] | undefined) => {
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
    this.close()
  }

  close() {
    if (this.alerts && this.alerts.length > 0) {
      this.alertService.clear(this.alerts[0])
      this.cdr.detectChanges()
    }
  }
}
