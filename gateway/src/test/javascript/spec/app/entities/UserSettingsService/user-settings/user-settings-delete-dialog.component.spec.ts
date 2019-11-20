/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { GatewayTestModule } from '../../../../test.module';
import { UserSettingsDeleteDialogComponent } from 'app/entities/UserSettingsService/user-settings/user-settings-delete-dialog.component';
import { UserSettingsService } from 'app/entities/UserSettingsService/user-settings/user-settings.service';

describe('Component Tests', () => {
  describe('UserSettings Management Delete Component', () => {
    let comp: UserSettingsDeleteDialogComponent;
    let fixture: ComponentFixture<UserSettingsDeleteDialogComponent>;
    let service: UserSettingsService;
    let mockEventManager: any;
    let mockActiveModal: any;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [UserSettingsDeleteDialogComponent]
      })
        .overrideTemplate(UserSettingsDeleteDialogComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(UserSettingsDeleteDialogComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(UserSettingsService);
      mockEventManager = fixture.debugElement.injector.get(JhiEventManager);
      mockActiveModal = fixture.debugElement.injector.get(NgbActiveModal);
    });

    describe('confirmDelete', () => {
      it('Should call delete service on confirmDelete', inject(
        [],
        fakeAsync(() => {
          // GIVEN
          spyOn(service, 'delete').and.returnValue(of({}));

          // WHEN
          comp.confirmDelete('123');
          tick();

          // THEN
          expect(service.delete).toHaveBeenCalledWith('123');
          expect(mockActiveModal.dismissSpy).toHaveBeenCalled();
          expect(mockEventManager.broadcastSpy).toHaveBeenCalled();
        })
      ));
    });
  });
});
