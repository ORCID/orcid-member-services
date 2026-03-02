import { Moment } from 'moment'
import { EventType } from 'src/app/app.constants'

export class OrcidRecord {
  constructor(
    public email: string,
    public orcid: string,
    public tokens?: any,
    public last_notified?: Moment,
    public revoke_notification_sent_date?: Moment,
    public eminder_notification_sent_date?: Moment,
    public created?: Moment,
    public modified?: Moment
  ) {}
}
