import { Pipe, PipeTransform, inject } from '@angular/core'
import { LanguageService } from '../service/language.service'

@Pipe({
  name: 'findLanguageFromKey',
  standalone: false,
})
export class FindLanguageFromKeyPipe implements PipeTransform {
  private languageService = inject(LanguageService)

  transform(lang: string): string {
    return this.languageService.getAllLanguages()[lang].name
  }
}
