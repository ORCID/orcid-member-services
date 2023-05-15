import { Component, HostListener } from '@angular/core';

@Component({
  selector: 'app-contact-update-confirmation-alert',
  templateUrl: './contact-update-confirmation-alert.component.html',
  styleUrls: ['contact-update-confirmation-alert.scss']
})
export class ContactUpdateConfirmationAlert {
  alerts: any[];
  constructor() {}

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKeydown() {
    this.hide();
  }

  hide() {
    // handled in the alert service
  }
}
