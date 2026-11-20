import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authenticatedModule } from '../../../../test-support/authenticated-module';
import { usuarioFixture } from '../../../../test-support/usuario-fixture';
import { UsuariosListPageComponent } from './usuarios-list-page.component';

describe('Listagem administrativa', () => {
  it('identifica a conta por monograma decorativo ao lado do username completo', () => {
    authenticatedModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(UsuariosListPageComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/usuarios').flush([usuarioFixture({ username: 'ana.silva' })]); fixture.detectChanges();
    const row: HTMLElement = fixture.nativeElement.querySelector('tbody tr');
    expect(row.querySelector('[aria-hidden="true"]')?.textContent?.trim()).toBe('AS');
    expect(row.querySelector('th')?.textContent).toContain('ana.silva');
    expect(row.querySelector('a')?.getAttribute('aria-label')).toBe('Consultar usuário ana.silva');
    http.verify();
  });
  it.each([['alice', 'AL'], ['equipe__sul', 'ES'], ['---', '--']])('preserva a identificação de %s com monograma %s', (username, monogram) => {
    authenticatedModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(UsuariosListPageComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/usuarios').flush([usuarioFixture({ username })]); fixture.detectChanges();
    const row: HTMLElement = fixture.nativeElement.querySelector('tbody tr');
    expect(row.querySelector('[aria-hidden="true"]')?.textContent?.trim()).toBe(monogram);
    expect(row.querySelector('th')?.textContent).toContain(username);
    http.verify();
  });
  it('agrupa resumo e tabela numa região identificada e preserva todos os dados da conta', () => {
    authenticatedModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(UsuariosListPageComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const username = 'conta.administrativa.' + 'a'.repeat(79);
    http.expectOne('/api/usuarios').flush([usuarioFixture({ username, ativo: false, credencialDefinida: false })]);
    fixture.detectChanges();
    const region: HTMLElement | null = fixture.nativeElement.querySelector('[role="region"][aria-labelledby="contas-title"]');
    expect(region).not.toBeNull();
    expect(region?.querySelector('#contas-title')?.textContent).toBe('Contas cadastradas');
    expect(region?.querySelector('[aria-label="Resumo de usuários"]')).not.toBeNull();
    const rows = region?.querySelectorAll('tbody tr');
    expect(rows?.length).toBe(1);
    expect(rows?.[0].textContent).toContain(username);
    expect(rows?.[0].textContent).toContain('USER');
    expect(rows?.[0].textContent).toContain('Bloqueado');
    expect(rows?.[0].textContent).toContain('Pendente');
    expect(rows?.[0].querySelector('a')?.getAttribute('aria-label')).toBe('Consultar usuário ' + username);
    http.verify();
  });
  it('oferece quebras após separadores preservando username completo e ordem recebida', () => {
    authenticatedModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(UsuariosListPageComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/usuarios').flush([
      usuarioFixture({ id: 9, username: 'financeiro_equipe-sul.2026' }),
      usuarioFixture({ id: 2, username: 'a'.repeat(100) })
    ]); fixture.detectChanges();
    const rows: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('tbody tr');
    expect([...rows].map(row => row.querySelector('a')?.getAttribute('href'))).toEqual(['/usuarios/9', '/usuarios/2']);
    expect(rows[0].querySelector('th')?.textContent).toContain('financeiro_equipe-sul.2026');
    expect(rows[1].querySelector('th')?.textContent).toContain('a'.repeat(100));
    const breaks = [...rows[0].querySelectorAll('wbr')];
    expect(breaks.length).toBeGreaterThan(0);
    expect(breaks.every(br => /[._-]$/.test(br.previousSibling?.textContent ?? ''))).toBe(true);
    http.verify();
  });
  it('resume todas as contas recebidas na listagem sem outra consulta', () => {
    authenticatedModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(UsuariosListPageComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/usuarios').flush([
      usuarioFixture({ id: 7, username: 'alice', ativo: true }),
      usuarioFixture({ id: 8, username: 'bruno', ativo: true }),
      usuarioFixture({ id: 9, username: 'carla', ativo: false })
    ]);
    fixture.detectChanges();
    const summary = fixture.nativeElement.querySelector('[aria-label="Resumo de usuários"]');
    expect(summary?.textContent).toMatch(/Total de usuários\s*3/);
    expect(summary?.textContent).toMatch(/Usuários ativos\s*2/);
    expect(summary?.textContent).toMatch(/Usuários bloqueados\s*1/);
    http.verify();
  });
  it('ADMIN consulta contas e abre detalhe sem campos para senhas ou códigos', () => {
    authenticatedModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(UsuariosListPageComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController); http.expectOne('/api/usuarios').flush([usuarioFixture()]); fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('h1')?.textContent).toBe('Usuários');
    expect(element.querySelector('tbody')?.textContent).toContain('alice');
    expect(element.querySelector('a[href="/usuarios/7"]')).not.toBeNull();
    expect(element.querySelector('a[href="/usuarios/novo"]')).not.toBeNull();
    expect(element.querySelector('input[type=password]')).toBeNull(); http.verify();
  });
  it('não apresenta contagens como zero enquanto carrega ou quando a consulta falha', () => {
    authenticatedModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(UsuariosListPageComponent); fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const http = TestBed.inject(HttpTestingController);
    expect(element.querySelector('[aria-label="Resumo de usuários"]')).toBeNull();
    http.expectOne('/api/usuarios').flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(element.querySelector('[aria-label="Resumo de usuários"]')).toBeNull();
    expect(element.textContent).toContain('Não foi possível consultar usuários.');
    http.verify();
  });
  it('apresenta zero somente para uma coleção completa vazia com sucesso', () => {
    authenticatedModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(UsuariosListPageComponent); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/usuarios').flush([]); fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect([...element.querySelectorAll('[aria-label="Resumo de usuários"] dd')].map(dd => dd.textContent?.trim())).toEqual(['0', '0', '0']);
    expect(element.textContent).toContain('Nenhuma conta disponível para consulta.');
    http.verify();
  });
});
