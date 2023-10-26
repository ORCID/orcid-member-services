import { Directive, Input, TemplateRef, ViewContainerRef } from '@angular/core'
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
})
export class HasAnyAuthorityDirective {
  private authorities: string[] = []

  constructor(
    private accountService: AccountService,
    private templateRef: TemplateRef<any>,
    private viewContainerRef: ViewContainerRef
  ) {}

  @Input()
  set hasAnyAuthority(value: string | string[]) {
    this.authorities = typeof value === 'string' ? [value] : value
    this.updateView()
    // Get notified each time authentication state changes.
    this.accountService.getAuthenticationState().subscribe((identity) => this.updateView())
  }

  private updateView(): void {
    const hasAnyAuthority = this.accountService.hasAnyAuthority(this.authorities)
    this.viewContainerRef.clear()
    if (hasAnyAuthority) {
      this.viewContainerRef.createEmbeddedView(this.templateRef)
    }
  }
}
