/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { Observable, of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { AffiliationUpdateComponent } from 'app/entities/AssertionServices/affiliation/affiliation-update.component';
import { AffiliationService } from 'app/entities/AssertionServices/affiliation/affiliation.service';
import { Affiliation } from 'app/shared/model/AssertionServices/affiliation.model';

describe('Component Tests', () => {
  describe('Affiliation Management Update Component', () => {
    let comp: AffiliationUpdateComponent;
    let fixture: ComponentFixture<AffiliationUpdateComponent>;
    let service: AffiliationService;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [AffiliationUpdateComponent],
        providers: [FormBuilder]
      })
        .overrideTemplate(AffiliationUpdateComponent, '')
        .compileComponents();

      fixture = TestBed.createComponent(AffiliationUpdateComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(AffiliationService);
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', fakeAsync(() => {
        // GIVEN
        const entity = new Affiliation('123');
        spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
        comp.updateForm(entity);
        // WHEN
        comp.save();
        tick(); // simulate async

        // THEN
        expect(service.update).toHaveBeenCalledWith(entity);
        expect(comp.isSaving).toEqual(false);
      }));

      it('Should call create service on save for new entity', fakeAsync(() => {
        // GIVEN
        const entity = new Affiliation();
        spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
        comp.updateForm(entity);
        // WHEN
        comp.save();
        tick(); // simulate async

        // THEN
        expect(service.create).toHaveBeenCalledWith(entity);
        expect(comp.isSaving).toEqual(false);
      }));
    });
  });
});
