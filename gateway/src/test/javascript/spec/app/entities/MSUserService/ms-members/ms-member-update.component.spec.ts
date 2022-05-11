/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { Observable, of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { MSMemberUpdateComponent } from 'app/entities/member/member-update.component';
import { MSMemberService } from 'app/entities/member/member.service';
import { MSMember } from 'app/shared/model/member.model';

describe('Component Tests', () => {
  describe('MSMember Management Update Component', () => {
    let comp: MSMemberUpdateComponent;
    let fixture: ComponentFixture<MSMemberUpdateComponent>;
    let service: MSMemberService;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MSMemberUpdateComponent],
        providers: [FormBuilder]
      })
        .overrideTemplate(MSMemberUpdateComponent, '')
        .compileComponents();

      fixture = TestBed.createComponent(MSMemberUpdateComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(MSMemberService);
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', fakeAsync(() => {
        // GIVEN
        const entity = new MSMember('123');
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
        const entity = new MSMember();
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
