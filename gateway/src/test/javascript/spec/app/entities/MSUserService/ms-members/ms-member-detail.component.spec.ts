/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { MSMemberDetailComponent } from 'app/entities/member/member-detail.component';
import { MSMember } from 'app/shared/model/member.model';

describe('Component Tests', () => {
  describe('MSMember Management Detail Component', () => {
    let comp: MSMemberDetailComponent;
    let fixture: ComponentFixture<MSMemberDetailComponent>;
    const route = ({ data: of({ msMember: new MSMember('123') }) } as any) as ActivatedRoute;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MSMemberDetailComponent],
        providers: [{ provide: ActivatedRoute, useValue: route }]
      })
        .overrideTemplate(MSMemberDetailComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(MSMemberDetailComponent);
      comp = fixture.componentInstance;
    });

    describe('OnInit', () => {
      it('Should call load all on init', () => {
        // GIVEN

        // WHEN
        comp.ngOnInit();

        // THEN
        expect(comp.msMember).toEqual(jasmine.objectContaining({ id: '123' }));
      });
    });
  });
});
