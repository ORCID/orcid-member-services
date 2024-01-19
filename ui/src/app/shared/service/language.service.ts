import { Inject, Injectable } from '@angular/core'
import { Observable, of } from 'rxjs'
import { WindowLocationService } from './window-location.service'

@Injectable({ providedIn: 'root' })
export class LanguageService {
  constructor(private windowLocationService: WindowLocationService) {}

  private languages: { [langCode: string]: { name: string } } = {
    en: { name: 'English' },
    es: { name: 'Español' },
    fr: { name: 'Français' },
    ja: { name: '日本語' },
    'zh-TW': { name: '繁體中文' },
    'zh-CN': { name: '简体中文' },
    cs: { name: 'Čeština' },
    it: { name: 'Italiano' },
    ko: { name: '한국어' },
    pt: { name: 'Português' },
    ru: { name: 'Pусский' },
    xx: { name: 'Test' },
  }

  getAllLanguages(): { [langCode: string]: { name: string } } {
    return this.languages
  }

  getCurrentLanguage(): Observable<string> {
    return of('en')
  }

  changeLanguage(languageKey: string): void {
    console.log('not empty')
  }

  updateLanguageCodeInUrl(langCode: string) {
    if (!this.windowLocationService.getWindowLocationOrigin().includes('localhost')) {
      const currentLang = this.getLanguageCodeFromUrl()
      if (currentLang == langCode) {
        // current locale is correct
        return
      }

      // something has to be done
      if (currentLang == null) {
        // add langCode to url
        console.log('adding locale to url')
        this.windowLocationService.updateWindowLocation(
          this.windowLocationService.getWindowLocationOrigin() +
            '/' +
            langCode +
            this.windowLocationService.getWindowLocationPathname()
        )
      } else {
        // remove current lang code and replace with users lang
        console.log('changing locale in url')
        this.windowLocationService.updateWindowLocation(
          this.windowLocationService.getWindowLocationOrigin() +
            this.windowLocationService.getWindowLocationPathname().replace(currentLang, langCode)
        )
      }
    }
  }

  getLanguageCodeFromUrl() {
    let code: string | null = null
    Object.keys(this.getAllLanguages()).forEach((langCode: string) => {
      if (this.windowLocationService.getWindowLocationPathname().includes('/' + langCode + '/')) {
        code = langCode
        return
      }
    })
    return code
  }
}
