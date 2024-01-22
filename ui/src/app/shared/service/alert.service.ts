import { Injectable } from '@angular/core'
import { Observable, Subject } from 'rxjs'
import { AppAlert } from '../alert/model/alert.model'
import { AlertType } from 'src/app/app.constants'

@Injectable({ providedIn: 'root' })
export class AlertService {
  private alerts: Subject<any> = new Subject<any>()

  on(): Observable<any> {
    return this.alerts.asObservable()
  }

  broadcast(alert: string): void {
    const newAlert = new AppAlert('info', alert)
    this.alerts.next(newAlert)
    console.log('this.alerts.next called')
  }
}
