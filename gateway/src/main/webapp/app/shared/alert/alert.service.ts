import { ComponentType } from '@angular/cdk/portal';
import { ComponentFactoryResolver, ComponentRef, Injectable, Injector, ReflectiveInjector, ViewContainerRef } from '@angular/core';
import { ReplaySubject } from 'rxjs';
import { take } from 'rxjs/operators';

interface AlertInterface {
  alertComponent: ComponentType<any>;
  data?: any;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  alertRef: ComponentRef<any>;
  containerRefSubj = new ReplaySubject<ViewContainerRef>(1);

  constructor(private resolver: ComponentFactoryResolver) {}

  setupContainerRef(containerRef: ViewContainerRef) {
    this.containerRefSubj.next(containerRef);
  }

  showHomepageLightboxModal(alert: AlertInterface): void {
    this.containerRefSubj.pipe(take(1)).subscribe(containerRef => {
      if (alert.alertComponent) {
        const popupFactory = this.resolver.resolveComponentFactory(alert.alertComponent);
        const injector: Injector = ReflectiveInjector.resolveAndCreate([
          {
            provide: 'config',
            useValue: {
              data: alert.data
            }
          }
        ]);
        this.alertRef = containerRef.createComponent(popupFactory, 0, injector);
        this.alertRef.instance.hide = () => {
          this.hideHomepageLightboxModal();
        };
      }
    });
  }

  hideHomepageLightboxModal(): void {
    this.alertRef.destroy();
  }
}
