/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { MSUserDetailComponent } from 'app/entities/MSUserService/ms-users/ms-user-detail.component';
import { MSUser } from 'app/shared/model/MSUserService/ms-user.model';

describe('Component Tests', () => {
  describe('MSUser Management Detail Component', () => {
    let comp: MSUserDetailComponent;
    let fixture: ComponentFixture<MSUserDetailComponent>;
    const route = ({ data: of({ msUser: new MSUser('123') }) } as any) as ActivatedRoute;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [MSUserDetailComponent],
        providers: [{ provide: ActivatedRoute, useValue: route }]
      })
        .overrideTemplate(MSUserDetailComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(MSUserDetailComponent);
      comp = fixture.componentInstance;
    });

    describe('OnInit', () => {
      it('Should call load all on init', () => {
        // GIVEN

        // WHEN
        comp.ngOnInit();

        // THEN
        expect(comp.msUser).toEqual(jasmine.objectContaining({ id: '123' }));
      });
    });
  });
});
