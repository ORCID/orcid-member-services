/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing'
import { CUSTOM_ELEMENTS_SCHEMA, WritableSignal } from '@angular/core'

import { ActivatedRoute } from '@angular/router'
import { RouterModule } from '@angular/router'
import { of } from 'rxjs'
import { MemberDetailComponent } from './member-detail.component'
import { IMember } from './model/member.model'

type MemberDetailInternals = {
  member: WritableSignal<IMember | undefined>
}
const internals = (component: MemberDetailComponent): MemberDetailInternals =>
  component as unknown as MemberDetailInternals

describe('MemberDetailComponent', () => {
  let component: MemberDetailComponent
  let fixture: ComponentFixture<MemberDetailComponent>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>

  const defaultMember: IMember = {
    id: 'default',
    clientId: '',
    clientName: '',
    type: 'unset',
    status: 'unset',
  } as IMember

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), MemberDetailComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [{ provide: ActivatedRoute, useValue: { data: of({ member: defaultMember }) } }],
    })
    fixture = TestBed.createComponent(MemberDetailComponent)
    component = fixture.componentInstance
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should set member provided by activated route', () => {
    activatedRoute.data = of({
      member: {
        id: 'id',
        clientId: 'client-id',
        clientName: 'client-name',
      } as IMember,
    })

    component.ngOnInit()

    expect(internals(component).member()).toBeTruthy()
    expect(internals(component).member()!.id).toEqual('id')
    expect(internals(component).member()!.clientId).toEqual('client-id')
    expect(internals(component).member()!.clientName).toEqual('client-name')
  })
})
