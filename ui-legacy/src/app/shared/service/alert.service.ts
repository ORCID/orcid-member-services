import { Injectable } from '@angular/core'
import { BehaviorSubject, Observable, filter } from 'rxjs'
import { AppAlert } from '../alert/model/alert.model'
import { AlertType } from 'src/app/app.constants'

@Injectable({ providedIn: 'root' })
export class AlertService {
  private alerts: BehaviorSubject<AppAlert[] | undefined> = new BehaviorSubject<AppAlert[] | undefined>(undefined)

  on(): Observable<AppAlert[] | undefined> {
    return this.alerts.pipe(filter((alerts) => !!alerts))
  }

  broadcast(type: AlertType, msg?: string): void {
    const newAlert = new AppAlert(type, msg)
    const newAlerts = []
    if (this.alerts.value) {
      newAlerts.push(...this.alerts.value)
    }
    newAlerts.push(newAlert)

    this.alerts.next(newAlerts)

    if (type === AlertType.TOAST) {
      setTimeout(() => this.clear(newAlert), 5000)
    }
  }

  clear(alertToClear: AppAlert): void {
    const newAlerts = this.alerts.value?.filter((alert: any) => alert !== alertToClear)
    this.alerts.next(newAlerts)
  }
}
