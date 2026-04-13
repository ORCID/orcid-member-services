import { Directive, Input, TemplateRef, ViewContainerRef, inject } from '@angular/core'
import { AccountService } from 'src/app/account/service/account.service'

/**
 * @whatItDoes Conditionally includes an HTML element if current user has any
 * of the authorities passed as the `expression`.
 *
 * @howToUse
 * ```
 *     <some-element *hasAnyAuthority="'ROLE_ADMIN'">...</some-element>
 *
 *     <some-element *hasAnyAuthority="['ROLE_ADMIN', 'ROLE_USER']">...</some-element>
 * ```
 */
@Directive({
  selector: '[appHasAnyAuthority]',
  standalone: false,
})
export class HasAnyAuthorityDirective {
  private accountService = inject(AccountService)
  private templateRef = inject<TemplateRef<any>>(TemplateRef)
  private viewContainerRef = inject(ViewContainerRef)

  private authorities: string[] = []

  @Input()
  set appHasAnyAuthority(value: string | string[]) {
    this.authorities = typeof value === 'string' ? [value] : value
    this.updateView()
    // Get notified each time authentication state changes.
    this.accountService.getAccountData().subscribe((identity) => this.updateView())
  }

  private updateView(): void {
    const hasAnyAuthority = this.accountService.hasAnyAuthority(this.authorities)
    this.viewContainerRef.clear()
    if (hasAnyAuthority) {
      this.viewContainerRef.createEmbeddedView(this.templateRef)
    }
  }
}
