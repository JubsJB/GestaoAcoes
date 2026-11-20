import { TestBed } from '@angular/core/testing';
import { VisualSampleComponent } from './visual-sample.component';

describe('Amostra A — formulário demonstrativo local', () => {
  it('informa o campo obrigatório ao validar um exemplo vazio', async () => {
    const fixture = TestBed.createComponent(VisualSampleComponent);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Informe um nome para o exemplo.');
  });

  it('confirma um exemplo válido apenas na própria amostra', async () => {
    const fixture = TestBed.createComponent(VisualSampleComponent);
    fixture.detectChanges();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
    input.value = 'Minha amostra';
    input.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[role="status"]')?.textContent ?? '').toContain('Exemplo validado localmente. Nenhum dado foi enviado.');
  });
});
