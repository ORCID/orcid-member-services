/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { Observable, of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { MemberServicesUserUpdateComponent } from 'app/entities/UserSettingsService/member-services-user/member-services-user-update.component';
import { MemberServicesUserService } from 'app/entities/UserSettingsService/member-services-user/member-services-user.service';
import { MemberServicesUser } from 'app/shared/model/UserSettingsService/member-services-user.model';

describe('Component Tests', () => {
  describe('MemberServicesUser Management Update Component', () => {
    let comp: MemberServicesUserUpdateComponent;
    let fixture: ComponentFixture<MemberServicesUserUpdateComponent>;
    let service: MemberServicesUserService;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MemberServicesUserUpdateComponent],
        providers: [FormBuilder]
      })
        .overrideTemplate(MemberServicesUserUpdateComponent, '')
        .compileComponents();

      fixture = TestBed.createComponent(MemberServicesUserUpdateComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(MemberServicesUserService);
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', fakeAsync(() => {
        // GIVEN
        const entity = new MemberServicesUser('123');
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
        const entity = new MemberServicesUser();
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
