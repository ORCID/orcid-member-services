import { Injectable } from '@angular/core'
import { Observable, Subject } from 'rxjs'
import { AppAlert } from '../alert/model/alert.model'

@Injectable({ providedIn: 'root' })
export class AlertService {
  private alerts: Subject<any> = new Subject<any>()

  on(): Observable<any> {
    return this.alerts.asObservable()
  }

  broadcast(i18nKey: string): void {
    const newAlert = new AppAlert('info', i18nKey)
    this.alerts.next(newAlert)
    console.log('this.alerts.next called')
  }
}
