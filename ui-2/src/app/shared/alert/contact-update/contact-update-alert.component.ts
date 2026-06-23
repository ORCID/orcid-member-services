import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, inject, signal, OnInit } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { map } from 'rxjs'
import { AlertService } from '../../service/alert.service'
import { AlertType } from 'src/app/app.constants'
import { AppAlert } from '../model/alert.model'

@Component({
  selector: 'app-contact-update-alert',
  templateUrl: './contact-update-alert.component.html',
  styleUrls: ['../overlay-modal.scss'],
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContactUpdateAlertComponent implements OnInit {
  private readonly alertService = inject(AlertService)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly alertsState = signal<AppAlert[] | undefined>(undefined)
  private readonly message = signal<any>(null)

  protected get alerts(): AppAlert[] | undefined {
    return this.alertsState()
  }

  ngOnInit(): void {
    this.alertService
      .on()
      .pipe(
        map((alerts) => alerts?.filter((alert) => alert.type === AlertType.CONTACT_UPDATED)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((alerts: AppAlert[] | undefined) => {
        this.alertsState.set(alerts)
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
