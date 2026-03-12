import { Pipe, PipeTransform } from '@angular/core'
import { LanguageService } from '../service/language.service'

@Pipe({ name: 'findLanguageFromKey' })
export class FindLanguageFromKeyPipe implements PipeTransform {
  constructor(private languageService: LanguageService) {}

  transform(lang: string): string {
    return this.languageService.getAllLanguages()[lang].name
  }
}
