import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { AppError, ErrorAlert } from './model/error.model'
import { ErrorService } from './service/error.service'
import { NgbAlertModule } from '@ng-bootstrap/ng-bootstrap'

@Component({
  selector: 'app-alert-error',
  templateUrl: './error-alert.component.html',
  imports: [NgbAlertModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorAlertComponent implements OnInit {
  private readonly errorService = inject(ErrorService)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly alertsState = signal<any[]>([])

  protected get alerts(): any[] {
    return this.alertsState()
  }

  ngOnInit(): void {
    this.errorService.on().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((err: AppError) => {
      const alert: ErrorAlert = {
        type: 'danger',
        msg: err.message,
        toast: false,
      }
      this.alertsState.update((currentAlerts) => [...currentAlerts, alert])
    })
  }

  @HostListener('document:keyup.escape')
  protected closeOldestAlert() {
    this.alertsState.update((currentAlerts) => currentAlerts.slice(1))
  }

  protected close(alertToRemove: any) {
    this.alertsState.update((currentAlerts) => currentAlerts.filter((alert: any) => alert !== alertToRemove))
  }
}
