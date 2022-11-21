import { Injectable } from '@angular/core';
import { Data, Params } from '@angular/router';
import { SessionStorageService } from 'ngx-webstorage';

type destinationStateParams = {
  destination: {
    name: string,
    data: Data
  },
  params: Params,
  from: {
    name: string
  }
}

@Injectable({ providedIn: 'root' })
export class StateStorageService {
  constructor(private $sessionStorage: SessionStorageService) {}

  getPreviousState() {
    return this.$sessionStorage.retrieve('previousState');
  }

  resetPreviousState() {
    this.$sessionStorage.clear('previousState');
  }

  storePreviousState(previousStateName: string, previousStateParams: string) {
    const previousState = { name: previousStateName, params: previousStateParams };
    this.$sessionStorage.store('previousState', previousState);
  }

  getDestinationState() {
    return this.$sessionStorage.retrieve('destinationState');
  }

  storeUrl(url: string | null) {
    this.$sessionStorage.store('previousUrl', url);
  }

  getUrl() {
    return this.$sessionStorage.retrieve('previousUrl');
  }

  storeDestinationState (destinationInfo: destinationStateParams)  {
    this.$sessionStorage.store('destinationState', destinationInfo);
  }
}
