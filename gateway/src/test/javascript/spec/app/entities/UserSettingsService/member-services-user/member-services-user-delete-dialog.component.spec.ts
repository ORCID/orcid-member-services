/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { GatewayTestModule } from '../../../../test.module';
import { MemberServicesUserDeleteDialogComponent } from 'app/entities/UserSettingsService/member-services-user/member-services-user-delete-dialog.component';
import { MemberServicesUserService } from 'app/entities/UserSettingsService/member-services-user/member-services-user.service';

describe('Component Tests', () => {
  describe('MemberServicesUser Management Delete Component', () => {
    let comp: MemberServicesUserDeleteDialogComponent;
    let fixture: ComponentFixture<MemberServicesUserDeleteDialogComponent>;
    let service: MemberServicesUserService;
    let mockEventManager: any;
    let mockActiveModal: any;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MemberServicesUserDeleteDialogComponent]
      })
        .overrideTemplate(MemberServicesUserDeleteDialogComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(MemberServicesUserDeleteDialogComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(MemberServicesUserService);
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
