import { ComponentType } from '@angular/cdk/portal';
import { ComponentFactoryResolver, ComponentRef, Injectable, ViewContainerRef } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  activeAlert: BehaviorSubject<ComponentType<any>> = new BehaviorSubject(null);
  alertRef: ComponentRef<any>;

  constructor(private resolver: ComponentFactoryResolver) {}

  showHomepageLightboxModal(containerRef: ViewContainerRef): void {
    console.log(this.activeAlert.value);

    if (this.activeAlert.value) {
      const popupFactory = this.resolver.resolveComponentFactory(this.activeAlert.value);
      this.alertRef = containerRef.createComponent(popupFactory);
      this.alertRef.instance.hide = () => {
        this.hideHomepageLightboxModal();
      };
    }
  }

  hideHomepageLightboxModal(): void {
    this.alertRef.destroy();
    this.activeAlert.next(null);
  }
}
