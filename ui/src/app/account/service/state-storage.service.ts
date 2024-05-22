import { Injectable } from '@angular/core'
import { SessionStorageService } from 'ngx-webstorage'

@Injectable({ providedIn: 'root' })
export class StateStorageService {
  constructor(private $sessionStorage: SessionStorageService) {}

  getPreviousState() {
    return this.$sessionStorage.retrieve('previousState')
  }

  resetPreviousState() {
    this.$sessionStorage.clear('previousState')
  }

  getDestinationState() {
    return this.$sessionStorage.retrieve('destinationState')
  }

  storeUrl(url: string | null) {
    this.$sessionStorage.store('previousUrl', url)
  }

  getUrl() {
    return this.$sessionStorage.retrieve('previousUrl')
  }
}
