import { Component, HostListener, Inject } from '@angular/core';

@Component({
  selector: 'add-consortium-member-confirmation',
  templateUrl: './add-consortium-member-confirmation.component.html',
  styleUrls: ['../lightbox-modal.scss']
})
export class AddConsortiumMemberConfirmationComponent {
  alerts: any[];
  orgName: string;
  constructor(@Inject('config') config) {
    this.orgName = config.data;
  }

  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKeydown() {
    this.hide();
  }

  hide() {
    // handled in the alert service
  }
}
