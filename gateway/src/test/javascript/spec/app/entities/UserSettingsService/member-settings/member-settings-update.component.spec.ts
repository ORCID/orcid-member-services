/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { Observable, of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { MemberSettingsUpdateComponent } from 'app/entities/UserSettingsService/member-settings/member-settings-update.component';
import { MemberSettingsService } from 'app/entities/UserSettingsService/member-settings/member-settings.service';
import { MemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';

describe('Component Tests', () => {
  describe('MemberSettings Management Update Component', () => {
    let comp: MemberSettingsUpdateComponent;
    let fixture: ComponentFixture<MemberSettingsUpdateComponent>;
    let service: MemberSettingsService;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MemberSettingsUpdateComponent],
        providers: [FormBuilder]
      })
        .overrideTemplate(MemberSettingsUpdateComponent, '')
        .compileComponents();

      fixture = TestBed.createComponent(MemberSettingsUpdateComponent);
      comp = fixture.componentInstance;
      service = fixture.debugElement.injector.get(MemberSettingsService);
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', fakeAsync(() => {
        // GIVEN
        const entity = new MemberSettings('123');
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
        const entity = new MemberSettings();
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
