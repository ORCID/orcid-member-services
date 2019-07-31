/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { MemberServicesUserDetailComponent } from 'app/entities/UserSettingsService/member-services-user/member-services-user-detail.component';
import { MemberServicesUser } from 'app/shared/model/UserSettingsService/member-services-user.model';

describe('Component Tests', () => {
  describe('MemberServicesUser Management Detail Component', () => {
    let comp: MemberServicesUserDetailComponent;
    let fixture: ComponentFixture<MemberServicesUserDetailComponent>;
    const route = ({ data: of({ memberServicesUser: new MemberServicesUser('123') }) } as any) as ActivatedRoute;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MemberServicesUserDetailComponent],
        providers: [{ provide: ActivatedRoute, useValue: route }]
      })
        .overrideTemplate(MemberServicesUserDetailComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(MemberServicesUserDetailComponent);
      comp = fixture.componentInstance;
    });

    describe('OnInit', () => {
      it('Should call load all on init', () => {
        // GIVEN

        // WHEN
        comp.ngOnInit();

        // THEN
        expect(comp.memberServicesUser).toEqual(jasmine.objectContaining({ id: '123' }));
      });
    });
  });
});
