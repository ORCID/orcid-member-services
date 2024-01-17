import { ComponentFixture, TestBed } from '@angular/core/testing'

import { UsersComponent } from './users.component'
import { ActivatedRoute, Data } from '@angular/router'
import { UserPage } from './model/user-page.model'
import { User } from './model/user.model'
import { UserService } from './service/user.service'
import { of } from 'rxjs'
import { AppModule } from '../app.module'

describe('UsersComponent', () => {
  let component: UsersComponent
  let fixture: ComponentFixture<UsersComponent>
  let service: UserService

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppModule],
      declarations: [UsersComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: {
              subscribe: (fn: (value: Data) => void) =>
                fn({
                  pagingParams: {
                    predicate: 'id',
                    reverse: false,
                    page: 0,
                  },
                }),
            },
          },
        },
      ],
    })
      .overrideTemplate(UsersComponent, '')
      .compileComponents()
    fixture = TestBed.createComponent(UsersComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('Should call load all on init', () => {
    // GIVEN
    spyOn(service, 'query').and.returnValue(of(new UserPage([new User('123')], 20)))

    // WHEN
    component.ngOnInit()

    // THEN
    expect(service.query).toHaveBeenCalled()
    expect(component.users && component.users[0]).toEqual(jasmine.objectContaining({ id: '123' }))
  })

  it('should load a page', () => {
    spyOn(service, 'query').and.returnValue(of(new UserPage([new User('123')], 20)))

    // WHEN
    component.loadPage(1)

    // THEN
    expect(service.query).toHaveBeenCalled()
    expect(component.users![0]).toEqual(jasmine.objectContaining({ id: '123' }))
  })

  it('should not load a page is the page is the same as the previous page', () => {
    spyOn(service, 'query').and.callThrough()

    // WHEN
    component.loadPage(0)

    // THEN
    expect(service.query).toHaveBeenCalledTimes(0)
  })

  it('should re-initialize the page', () => {
    spyOn(service, 'query').and.returnValue(of(new UserPage([new User('123')], 20)))

    // WHEN
    component.loadPage(1)
    component.clear()

    // THEN
    expect(component.page).toEqual(0)
    expect(service.query).toHaveBeenCalledTimes(2)
    expect(component.users && component.users[0]).toEqual(jasmine.objectContaining({ id: '123' }))
  })
  it('should calculate the sort attribute for an id', () => {
    // WHEN
    const result = component.sort()

    // THEN
    expect(result).toEqual(['id,desc'])
  })

  it('should calculate the sort attribute for a non-id attribute', () => {
    // GIVEN
    component.predicate = 'name'

    // WHEN
    const result = component.sort()

    // THEN
    expect(result).toEqual(['name,desc', 'id'])
  })
})
