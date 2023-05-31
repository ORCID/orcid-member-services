import { Component, HostListener } from '@angular/core';

@Component({
  selector: 'remove-consortium-member-confirmation',
  templateUrl: './remove-consortium-member-confirmation.component.html',
  styleUrls: ['../lightbox-modal.scss']
})
export class RemoveConsortiumMemberConfirmationComponent {
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
