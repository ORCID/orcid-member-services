import { Component, HostListener } from '@angular/core';

@Component({
  selector: 'add-consortium-member-confirmation',
  templateUrl: './add-consortium-member-confirmation.component.html',
  styleUrls: ['add-consortium-member-confirmation.scss']
})
export class AddConsortiumMemberConfirmationComponent {
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
