import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { map } from 'rxjs'
import { AlertService } from '../service/alert.service'
import { AppAlert } from './model/alert.model'
import { AlertType } from 'src/app/app.constants'
import { NgbAlertModule } from '@ng-bootstrap/ng-bootstrap'
import { LocalizePipe } from '../pipe/localize'

@Component({
  selector: 'app-alert-toast',
  templateUrl: './alert-toast.component.html',
  imports: [NgbAlertModule, LocalizePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertComponent implements OnInit {
  private readonly alertService = inject(AlertService)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly alertsState = signal<any[]>([])
  private readonly message = signal<any>(null)

  protected get alerts(): any[] {
    return this.alertsState()
  }

  ngOnInit(): void {
    this.alertService
      .on()
      .pipe(
        map((alerts) => alerts?.filter((alert) => alert.type === AlertType.TOAST) as AppAlert[]),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((alerts: AppAlert[]) => {
        this.alertsState.set(alerts)
      })
  }

  @HostListener('document:keyup.escape')
  @HostListener('document:keyup.enter')
  protected closeOldestAlert() {
    this.alertService.clear(this.alertsState()[0])
  }

  protected close(alertToRemove: any) {
    this.alertService.clear(alertToRemove)
  }
}
