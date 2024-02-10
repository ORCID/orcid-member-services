import { Injectable } from '@angular/core'
import { BehaviorSubject, Observable, Subject, filter } from 'rxjs'
import { AppAlert } from '../alert/model/alert.model'

@Injectable({ providedIn: 'root' })
export class AlertService {
  private alerts: BehaviorSubject<AppAlert[] | undefined> = new BehaviorSubject<AppAlert[] | undefined>(undefined)

  on(): Observable<any> {
    return this.alerts.pipe(filter((alerts) => !!alerts))
  }

  broadcast(alert: string): void {
    const newAlert = new AppAlert('info', alert)
    const newAlerts = []
    if (this.alerts.value) {
      newAlerts.push(...this.alerts.value)
    }
    newAlerts.push(newAlert)

    this.alerts.next(newAlerts)
    setTimeout(() => this.clear(newAlert), 5000)
  }

  clear(alertToClear: AppAlert): void {
    const newAlerts = this.alerts.value?.filter((alert: any) => alert !== alertToClear)

    this.alerts.next(newAlerts)
  }
}
