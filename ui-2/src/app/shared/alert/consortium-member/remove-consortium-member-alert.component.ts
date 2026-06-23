import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, inject, signal, OnInit } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { AppAlert } from '../model/alert.model'
import { map } from 'rxjs'
import { AlertService } from '../../service/alert.service'
import { AlertType } from 'src/app/app.constants'

@Component({
  selector: 'app-remove-consortium-member-alert',
  templateUrl: './remove-consortium-member-alert.component.html',
  styleUrls: ['../overlay-modal.scss'],
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RemoveConsortiumMemberAlertComponent implements OnInit {
  private readonly alertService = inject(AlertService)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly alertsState = signal<AppAlert[] | undefined>(undefined)
  protected readonly orgNameState = signal('')

  protected get alerts(): AppAlert[] | undefined {
    return this.alertsState()
  }

  protected get orgName(): string {
    return this.orgNameState()
  }

  ngOnInit(): void {
    this.alertService
      .on()
      .pipe(
        map((alerts) => alerts?.filter((alert) => alert.type === AlertType.CONSORTIUM_MEMBER_REMOVED)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((alerts: AppAlert[] | undefined) => {
        this.alertsState.set(alerts)
        if (alerts && alerts.length > 0 && alerts[0].msg) {
          this.orgNameState.set(alerts[0].msg)
        }
      })
  }

  @HostListener('document:keyup.escape')
  @HostListener('document:keyup.enter')
  protected closeOldestAlert() {
    this.close()
  }

  protected close() {
    const currentAlerts = this.alertsState()
    if (currentAlerts && currentAlerts.length > 0) {
      this.alertService.clear(currentAlerts[0])
    }
  }
}
