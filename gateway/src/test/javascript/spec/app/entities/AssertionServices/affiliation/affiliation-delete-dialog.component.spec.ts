/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { GatewayTestModule } from '../../../../test.module';
import { AffiliationDeleteDialogComponent } from 'app/entities/AssertionServices/affiliation/affiliation-delete-dialog.component';
import { AffiliationService } from 'app/entities/AssertionServices/affiliation/affiliation.service';

describe('Component Tests', () => {
  describe('Affiliation Management Delete Component', () => {
    let comp: AffiliationDeleteDialogComponent;
    let fixture: ComponentFixture<AffiliationDeleteDialogComponent>;
    let service: AffiliationService;
    let mockEventManager: any;
    let mockActiveModal: any;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [AffiliationDeleteDialogComponent]
      })
        .overrideTemplate(AffiliationDeleteDialogComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(AffiliationDeleteDialogComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(AffiliationService);
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
