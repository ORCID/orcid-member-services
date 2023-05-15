import { ComponentFactoryResolver, ComponentRef, Injectable, ViewContainerRef } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ContactUpdateConfirmationAlert } from './..';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  contactUpdated: BehaviorSubject<boolean> = new BehaviorSubject(false);
  contactUpdateConfirmationAlertRef: ComponentRef<ContactUpdateConfirmationAlert>;

  constructor(private resolver: ComponentFactoryResolver) {}

  showContactUpdateConfirmationAlert(containerRef: ViewContainerRef): void {
    const popupFactory = this.resolver.resolveComponentFactory(ContactUpdateConfirmationAlert);
    this.contactUpdateConfirmationAlertRef = containerRef.createComponent(popupFactory);
    this.contactUpdateConfirmationAlertRef.instance.hide = () => {
      this.hideContactUpdateConfirmationAlert();
    };
  }

  hideContactUpdateConfirmationAlert(): void {
    this.contactUpdateConfirmationAlertRef.destroy();
  }
}
