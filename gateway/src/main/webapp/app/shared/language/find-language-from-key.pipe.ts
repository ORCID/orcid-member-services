import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'findLanguageFromKey' })
export class FindLanguageFromKeyPipe implements PipeTransform {
  private languages: any = {
    en: { name: 'English' },
    es: { name: 'Español' },
    fr: { name: 'Français' },
    ja: { name: '日本語' },
    zh_TW: { name: '繁體中文' },
    zh_CN: { name: '<simplified-chinese>' },
    cs: { name: '<Czech>' },
    it: { name: '<Italian>' },
    ko: { name: '<Korean>' },
    pt: { name: '<Portuguese>' },
    ru: { name: '<Russian>' },
    xx: { name: 'Test' }
    // jhipster-needle-i18n-language-key-pipe - JHipster will add/remove languages in this object
  };
  transform(lang: string): string {
    return this.languages[lang].name;
  }
}
