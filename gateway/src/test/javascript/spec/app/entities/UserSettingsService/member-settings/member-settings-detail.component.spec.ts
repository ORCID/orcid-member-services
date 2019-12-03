/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { MemberSettingsDetailComponent } from 'app/entities/UserSettingsService/member-settings/member-settings-detail.component';
import { MemberSettings } from 'app/shared/model/UserSettingsService/member-settings.model';

describe('Component Tests', () => {
  describe('MemberSettings Management Detail Component', () => {
    let comp: MemberSettingsDetailComponent;
    let fixture: ComponentFixture<MemberSettingsDetailComponent>;
    const route = ({ data: of({ memberSettings: new MemberSettings('123') }) } as any) as ActivatedRoute;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MemberSettingsDetailComponent],
        providers: [{ provide: ActivatedRoute, useValue: route }]
      })
        .overrideTemplate(MemberSettingsDetailComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(MemberSettingsDetailComponent);
      comp = fixture.componentInstance;
    });

    describe('OnInit', () => {
      it('Should call load all on init', () => {
        // GIVEN

        // WHEN
        comp.ngOnInit();

        // THEN
        expect(comp.memberSettings).toEqual(jasmine.objectContaining({ id: '123' }));
      });
    });
  });
});
