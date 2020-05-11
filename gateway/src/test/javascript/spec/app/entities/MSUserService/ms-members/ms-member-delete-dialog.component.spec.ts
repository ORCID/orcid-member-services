/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { GatewayTestModule } from '../../../../test.module';
import { MSMemberDeleteDialogComponent } from 'app/entities/MSUserService/ms-members/ms-member-delete-dialog.component';
import { MSMemberService } from 'app/entities/MSUserService/ms-members/ms-member.service';

describe('Component Tests', () => {
  describe('MSMember Management Delete Component', () => {
    let comp: MSMemberDeleteDialogComponent;
    let fixture: ComponentFixture<MSMemberDeleteDialogComponent>;
    let service: MSMemberService;
    let mockEventManager: any;
    let mockActiveModal: any;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MSMemberDeleteDialogComponent]
      })
        .overrideTemplate(MSMemberDeleteDialogComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(MSMemberDeleteDialogComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(MSMemberService);
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
