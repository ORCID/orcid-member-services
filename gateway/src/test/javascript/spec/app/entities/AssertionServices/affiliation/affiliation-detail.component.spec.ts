/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { GatewayTestModule } from '../../../../test.module';
import { AffiliationDetailComponent } from 'app/entities/AssertionServices/affiliation/affiliation-detail.component';
import { Affiliation } from 'app/shared/model/AssertionServices/affiliation.model';

describe('Component Tests', () => {
  describe('Affiliation Management Detail Component', () => {
    let comp: AffiliationDetailComponent;
    let fixture: ComponentFixture<AffiliationDetailComponent>;
    const route = ({ data: of({ affiliation: new Affiliation('123') }) } as any) as ActivatedRoute;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [GatewayTestModule],
        declarations: [AffiliationDetailComponent],
        providers: [{ provide: ActivatedRoute, useValue: route }]
      })
        .overrideTemplate(AffiliationDetailComponent, '')
        .compileComponents();
      fixture = TestBed.createComponent(AffiliationDetailComponent);
      comp = fixture.componentInstance;
    });

    describe('OnInit', () => {
      it('Should call load all on init', () => {
        // GIVEN

        // WHEN
        comp.ngOnInit();

        // THEN
        expect(comp.affiliation).toEqual(jasmine.objectContaining({ id: '123' }));
      });
    });
  });
});
