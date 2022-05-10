/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { AssertionDetailComponent } from 'app/entities/assertion/assertion-detail.component';
import { Assertion } from 'app/shared/model/assertion.model';

describe('Component Tests', () => {
  describe('Assertion Management Detail Component', () => {
    let comp: AssertionDetailComponent;
    let fixture: ComponentFixture<AssertionDetailComponent>;
    const route = ({ data: of({ assertion: new Assertion('123') }) } as any) as ActivatedRoute;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [AssertionDetailComponent],
        providers: [{ provide: ActivatedRoute, useValue: route }]
      })
        .overrideTemplate(AssertionDetailComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(AssertionDetailComponent);
      comp = fixture.componentInstance;
    });

    describe('OnInit', () => {
      it('Should call load all on init', () => {
        // GIVEN

        // WHEN
        comp.ngOnInit();

        // THEN
        expect(comp.assertion).toEqual(jasmine.objectContaining({ id: '123' }));
      });
    });
  });
});
