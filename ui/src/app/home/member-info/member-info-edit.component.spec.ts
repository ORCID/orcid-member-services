import { ComponentFixture, TestBed } from '@angular/core/testing'

import { MemberInfoEditComponent } from './member-info-edit.component'
import { AppModule } from 'src/app/app.module'

describe('MemberInfoEditComponent', () => {
  let component: MemberInfoEditComponent
  let fixture: ComponentFixture<MemberInfoEditComponent>

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppModule],
      declarations: [MemberInfoEditComponent],
    })
    fixture = TestBed.createComponent(MemberInfoEditComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })
})
