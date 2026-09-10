import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TextProcessorComponent } from './text-processor-component';

describe('TextProcessorComponent', () => {
  let component: TextProcessorComponent;
  let fixture: ComponentFixture<TextProcessorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TextProcessorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TextProcessorComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
