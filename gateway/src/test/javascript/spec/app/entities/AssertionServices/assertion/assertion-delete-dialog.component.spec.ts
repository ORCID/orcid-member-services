/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { GatewayTestModule } from '../../../../test.module';
import { AssertionDeleteDialogComponent } from 'app/entities/AssertionService/assertion/assertion-delete-dialog.component';
import { AssertionService } from 'app/entities/AssertionService/assertion/assertion.service';

describe('Component Tests', () => {
  describe('Assertion Management Delete Component', () => {
    let comp: AssertionDeleteDialogComponent;
    let fixture: ComponentFixture<AssertionDeleteDialogComponent>;
    let service: AssertionService;
    let mockEventManager: any;
    let mockActiveModal: any;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [AssertionDeleteDialogComponent]
      })
        .overrideTemplate(AssertionDeleteDialogComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(AssertionDeleteDialogComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(AssertionService);
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
          comp.confirmDelete();
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
