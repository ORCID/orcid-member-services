/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { Observable, of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { MSUserUpdateComponent } from 'app/entities/user/user-update.component';
import { MSUserService } from 'app/entities/user/user.service';
import { MSUser } from 'app/shared/model/user.model';

describe('Component Tests', () => {
  describe('MSUser Management Update Component', () => {
    let comp: MSUserUpdateComponent;
    let fixture: ComponentFixture<MSUserUpdateComponent>;
    let service: MSUserService;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MSUserUpdateComponent],
        providers: [FormBuilder]
      })
        .overrideTemplate(MSUserUpdateComponent, '')
        .compileComponents();

      fixture = TestBed.createComponent(MSUserUpdateComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(MSUserService);
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', fakeAsync(() => {
        // GIVEN
        const entity = new MSUser('123');
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
        const entity = new MSUser();
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
