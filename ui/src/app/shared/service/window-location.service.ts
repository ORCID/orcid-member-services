import { Injectable } from '@angular/core'

@Injectable({ providedIn: 'root' })
export class WindowLocationService {
  updateWindowLocation(location: string) {
    window.location.href = location
  }

  getWindowLocationOrigin(): string {
    return window.location.origin
  }

  getWindowLocationPathname(): string {
    return window.location.pathname
  }

  getWindowLocationHref(): string {
    return window.location.href
  }
}
