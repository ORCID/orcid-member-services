import { Injectable } from '@angular/core'
import { Observable, of } from 'rxjs'

@Injectable({ providedIn: 'root' })
export class LanguageService {
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
}
