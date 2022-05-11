/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { Observable, of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { AssertionUpdateComponent } from 'app/entities/assertion/assertion-update.component';
import { AssertionService } from 'app/entities/assertion/assertion.service';
import { Assertion } from 'app/shared/model/assertion.model';

describe('Component Tests', () => {
  describe('Assertion Management Update Component', () => {
    let comp: AssertionUpdateComponent;
    let fixture: ComponentFixture<AssertionUpdateComponent>;
    let service: AssertionService;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [AssertionUpdateComponent],
        providers: [FormBuilder]
      })
        .overrideTemplate(AssertionUpdateComponent, '')
        .compileComponents();

      fixture = TestBed.createComponent(AssertionUpdateComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(AssertionService);
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', fakeAsync(() => {
        // GIVEN
        const entity = new Assertion('123');
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
        const entity = new Assertion();
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
