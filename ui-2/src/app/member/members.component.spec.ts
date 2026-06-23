/// <reference types="jasmine" />

import { CUSTOM_ELEMENTS_SCHEMA, WritableSignal } from '@angular/core'
import { ComponentFixture, TestBed, tick } from '@angular/core/testing'
import { FormsModule } from '@angular/forms'

import { MembersComponent } from './members.component'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { AccountService } from '../account'
import { RouterModule } from '@angular/router'
import { MemberService } from './service/member.service'
import { of } from 'rxjs'
import { Member } from './model/member.model'
import { Router } from '@angular/router'
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'

type MembersInternals = {
  page: WritableSignal<number>
  searchTerm: WritableSignal<string>
  submittedSearchTerm: WritableSignal<string>
  sortColumn: WritableSignal<string>
}

const internals = (component: MembersComponent): MembersInternals =>
  component as unknown as MembersInternals

describe('MembersComponent', () => {
  let component: MembersComponent
  let fixture: ComponentFixture<MembersComponent>
  let accountServiceSpy: jasmine.SpyObj<AccountService>
  let memberServiceSpy: jasmine.SpyObj<MemberService>
  let routerSpy: jasmine.SpyObj<Router>

  beforeEach(() => {
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    memberServiceSpy = jasmine.createSpyObj('MemberService', ['query'])
    routerSpy = jasmine.createSpyObj('Router', ['navigate'])

    TestBed.configureTestingModule({
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      imports: [FormsModule, RouterModule.forRoot([]), MembersComponent],
      providers: [
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    })

    fixture = TestBed.createComponent(MembersComponent)
    component = fixture.componentInstance

    accountServiceSpy = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    memberServiceSpy = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>

    memberServiceSpy.query.and.returnValue(
      of({
        content: [new Member('id')],
        page: {
          totalElements: 1,
          number: 0,
          size: 20,
          totalPages: 1,
        },
      })
    )

    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['ROLE_USER'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        loggedAs: false,
        loginAs: 'sfid',
        mainContact: false,
        mfaEnabled: false,
        memberId: 'memberId',
        manageApiCredsEnabled: false,
      })
    )
    spyOn(routerSpy, 'navigate').and.returnValue(Promise.resolve(true))
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('load all should call member service query', () => {
    component.loadAll()
    expect(memberServiceSpy.query).toHaveBeenCalled()
  })

  it('clear should navigate with router and call member service query', () => {
    component.clear()
    expect(internals(component).page()).toEqual(0)
    expect(routerSpy.navigate).toHaveBeenCalledWith([
      '/members',
      Object({ page: 0, sort: 'salesforceId,asc', filter: '' }),
    ])
    expect(memberServiceSpy.query).toHaveBeenCalled()
  })

  it('resetSearch should reset search fields', () => {
    internals(component).page.set(50)
    internals(component).searchTerm.set('test')
    internals(component).submittedSearchTerm.set('testing')

    component.resetSearch()

    expect(internals(component).page()).toEqual(1)
    expect(internals(component).searchTerm()).toEqual('')
    expect(internals(component).submittedSearchTerm()).toEqual('')
  })

  it('submitSearch should set submittedSearchTerm to value of searchTerm and then call member service query', () => {
    internals(component).page.set(50)
    internals(component).searchTerm.set('new search')
    internals(component).submittedSearchTerm.set('previously submitted search')

    component.submitSearch()

    expect(internals(component).page()).toEqual(1)
    expect(internals(component).searchTerm()).toEqual('new search')
    expect(internals(component).submittedSearchTerm()).toEqual('new search')

    expect(memberServiceSpy.query).toHaveBeenCalled()
  })

  it('updateSort should set sortColumn and then call member service query', () => {
    internals(component).sortColumn.set('some sort column')

    component.updateSort('different column')

    expect(internals(component).sortColumn()).toEqual('different column')
    expect(memberServiceSpy.query).toHaveBeenCalled()
  })
})
