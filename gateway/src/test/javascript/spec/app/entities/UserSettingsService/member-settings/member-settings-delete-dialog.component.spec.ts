/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { GatewayTestModule } from '../../../../test.module';
import { MemberSettingsDeleteDialogComponent } from 'app/entities/UserSettingsService/member-settings/member-settings-delete-dialog.component';
import { MemberSettingsService } from 'app/entities/UserSettingsService/member-settings/member-settings.service';

describe('Component Tests', () => {
  describe('MemberSettings Management Delete Component', () => {
    let comp: MemberSettingsDeleteDialogComponent;
    let fixture: ComponentFixture<MemberSettingsDeleteDialogComponent>;
    let service: MemberSettingsService;
    let mockEventManager: any;
    let mockActiveModal: any;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MemberSettingsDeleteDialogComponent]
      })
        .overrideTemplate(MemberSettingsDeleteDialogComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(MemberSettingsDeleteDialogComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(MemberSettingsService);
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
