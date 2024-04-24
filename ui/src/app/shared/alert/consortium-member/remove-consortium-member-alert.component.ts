import { ChangeDetectorRef, Component, HostListener, Inject } from '@angular/core'
import { AppAlert } from '../model/alert.model'
import { Subscription, map } from 'rxjs'
import { AlertService } from '../../service/alert.service'
import { AlertType } from 'src/app/app.constants'

@Component({
  selector: 'app-remove-consortium-member-alert',
  templateUrl: './remove-consortium-member-alert.component.html',
  styleUrls: ['../overlay-modal.scss'],
})
export class RemoveConsortiumMemberAlertComponent {
  alerts: AppAlert[] | undefined
  sub: Subscription | undefined
  orgName: string = ''

  constructor(
    private alertService: AlertService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.sub = this.alertService
      .on()
      .pipe(map((alerts) => alerts?.filter((alert) => alert.type === AlertType.CONSORTIUM_MEMBER_REMOVED)))
      .subscribe((alerts: AppAlert[] | undefined) => {
        this.alerts = alerts
        if (alerts && alerts.length > 0 && alerts[0].msg) {
          this.orgName = alerts[0].msg
        }
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
