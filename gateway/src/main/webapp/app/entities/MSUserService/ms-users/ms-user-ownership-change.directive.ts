import { Directive, ElementRef, EventEmitter, Inject, OnInit, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Directive({
  selector: '[jhi-ownershipChange]'
})
export class MSUserOwnershipChangeDirective implements OnInit {
  @Output() then = new EventEmitter<boolean>();
  @Output() else = new EventEmitter<boolean>();

  question: String;

  constructor(@Inject(ElementRef) private element: ElementRef, translate: TranslateService) {
    this.question = translate.instant('gatewayApp.msUserServiceMSUser.changeOwnership.question.string');
  }

  ngOnInit(): void {
    const directive = this;
    this.element.nativeElement.onclick = function() {
      const result = confirm(directive.question.toString());
      if (result) {
        directive.then.emit(true);
      } else {
        directive.else.emit(true);
      }
    };
  }
}
