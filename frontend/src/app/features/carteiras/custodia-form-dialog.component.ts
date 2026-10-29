import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { AcoesService } from '../acoes/acoes.service';
import { AcaoResponse } from '../acoes/models/acao';
import { CorretorasService } from '../corretoras/corretoras.service';
import { Corretora } from '../corretoras/models/corretora';
import { CarteiraResponse } from './models/carteira';
import { CadastroCustodia, MovimentacaoCustodia, NaturezaCustodia } from './models/custodia';
import { CustodiaService } from './custodia.service';
import { idCustodia } from './custodia.contract';
import { civilDateValidator, normalizeDecimal, quantityValidator } from '../operacoes/operacao-validators';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { detalhesQuantitativos, mensagemQuantitativa } from '../../core/errors/quantitative-errors';
import { normalizeHttpError } from '../../core/http/http-error-normalizer';
import { HttpErrorResponse } from '@angular/common/http';
import { FeedbackAlertComponent } from '../../shared/feedback-alert/feedback-alert.component';
import { DistribuicaoCorretorasService } from './distribuicao-corretoras.service';
import { DistribuicaoCarteiraResponse } from './models/distribuicao-corretoras';
import { formatOperationQuantity } from '../operacoes/operacao-validators';

export interface CustodiaFormDialogData { carteira: CarteiraResponse }

@Component({ selector: 'app-custodia-form-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule, FeedbackAlertComponent],
  templateUrl: './custodia-form-dialog.component.html',
  styleUrls: ['../../shared/form/simple-form.scss', '../../shared/dialog/dialog.scss'],
  styles: [':host{display:block;min-width:0}.contexto{overflow-wrap:anywhere}.campos{display:grid;gap:.875rem;grid-template-columns:repeat(2,minmax(0,1fr))}.wide{grid-column:1/-1}@media(max-width:36rem){.campos{grid-template-columns:minmax(0,1fr)}}'],
  changeDetection: ChangeDetectionStrategy.OnPush })
export class CustodiaFormDialogComponent {
  private readonly data = inject<CustodiaFormDialogData>(MAT_DIALOG_DATA);
  protected readonly carteira = Object.freeze({ ...this.data.carteira });
  private readonly dialog = inject<MatDialogRef<CustodiaFormDialogComponent, MovimentacaoCustodia>>(MatDialogRef);
  private readonly service = inject(CustodiaService);
  private readonly actions = inject(AcoesService);
  private readonly brokers = inject(CorretorasService);
  private readonly distribution = inject(DistribuicaoCorretorasService);
  private readonly destroyRef = inject(DestroyRef);
  private encerrado = false;
  protected readonly acoes = signal<AcaoResponse[]>([]);
  protected readonly corretoras = signal<Corretora[]>([]);
  protected readonly carregando = signal(false);
  protected readonly erroReferencias = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<NormalizedHttpError | null>(null);
  private readonly distribuicao = signal<DistribuicaoCarteiraResponse | null>(null);
  protected readonly saldoCarregando = signal(false);
  protected readonly saldoIndisponivel = signal(false);
  protected readonly form = new FormGroup({ natureza: new FormControl<NaturezaCustodia>('TRANSFERENCIA', { nonNullable: true, validators: Validators.required }),
    acaoKey: new FormControl('', { nonNullable: true, validators: [Validators.required,
      c => this.acoes().some(a => this.chave(a) === c.value) ? null : { referencia: true }] }),
    corretoraOrigemId: new FormControl<number | null>(null), corretoraDestinoId: new FormControl<number | null>(null),
    quantidade: new FormControl('', { nonNullable: true, validators: [Validators.required, quantityValidator(() => this.acao()?.mercado ?? null)] }),
    dataEfetiva: new FormControl('', { nonNullable: true, validators: [Validators.required, civilDateValidator(() => this.acao()?.mercado ?? null)] }) },
    { validators: c => c.get('natureza')?.value === 'TRANSFERENCIA'
      && c.get('corretoraOrigemId')?.value !== null && c.get('corretoraOrigemId')?.value === c.get('corretoraDestinoId')?.value
      ? { corretorasIguais: true } : null });

  constructor() {
    this.form.controls.corretoraDestinoId.setValidators([Validators.required, c => this.corretoras().some(b => b.id === c.value) ? null : { referencia: true }]);
    this.origemPorNatureza();
    this.form.controls.natureza.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.origemPorNatureza());
    this.form.controls.acaoKey.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.form.controls.quantidade.updateValueAndValidity(); this.form.controls.dataEfetiva.updateValueAndValidity();
    });
    this.carregarReferencias();
    if (idCustodia(this.carteira.id)) {
      this.saldoCarregando.set(true);
      this.distribution.consultar(this.carteira.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: v => { this.saldoCarregando.set(false);
          if (v.carteiraId === this.carteira.id) this.distribuicao.set(v); else this.saldoIndisponivel.set(true); },
        error: () => { this.saldoCarregando.set(false); this.saldoIndisponivel.set(true); }
      });
    } else this.saldoIndisponivel.set(true);
    this.destroyRef.onDestroy(() => { this.encerrado = true; });
  }
  protected chave(a: AcaoResponse): string { return `${a.ticker}|${a.mercado}`; }
  protected acao(): AcaoResponse | undefined { return this.acoes().find(a => this.chave(a) === this.form.controls.acaoKey.value); }
  protected nome(b: Corretora): string { return b.nomeFantasia || b.razaoSocial; }
  protected saldoAtual(): string | null {
    const a = this.acao();
    if (!a) return null;
    const p = this.distribuicao()?.posicoes.find(p => p.acaoId === a.id && p.ticker === a.ticker && p.mercado === a.mercado);
    const origem = this.form.controls.natureza.value === 'ATRIBUICAO' ? null : this.form.controls.corretoraOrigemId.value;
    if (!p || this.form.controls.natureza.value === 'TRANSFERENCIA' && origem === null) return null;
    const saldo = p.corretoras.find(c => c.corretoraId === origem)?.quantidadeLiquida
      ?? p.diagnosticosCorretoras.find(c => c.corretoraId === origem)?.quantidadeFinal;
    return saldo === undefined ? null : formatOperationQuantity(saldo, a.mercado);
  }
  protected invalido(campo: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[campo]; return c.touched && c.invalid;
  }
  private origemPorNatureza(): void {
    const c = this.form.controls.corretoraOrigemId;
    if (this.form.controls.natureza.value === 'ATRIBUICAO') { c.clearValidators(); c.setValue(null); }
    else c.setValidators([Validators.required, x => this.corretoras().some(b => b.id === x.value) ? null : { referencia: true }]);
    c.updateValueAndValidity();
  }
  protected carregarReferencias(): void {
    if (this.carregando() || this.encerrado) return;
    this.carregando.set(true); this.erroReferencias.set(false);
    forkJoin({ acoes: this.actions.listar(), corretoras: this.brokers.listar() })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: refs => {
        this.acoes.set(refs.acoes); this.corretoras.set(refs.corretoras); this.carregando.set(false);
        this.form.controls.acaoKey.updateValueAndValidity(); this.form.controls.corretoraDestinoId.updateValueAndValidity(); this.origemPorNatureza();
      }, error: () => { this.carregando.set(false); this.erroReferencias.set(true); } });
  }
  protected bloqueado(): boolean {
    return this.encerrado || this.enviando() || this.carregando() || this.erroReferencias()
      || !idCustodia(this.carteira.id) || !this.acoes().length || !this.corretoras().length;
  }
  protected cancelar(): void { if (!this.enviando()) this.dialog.close(); }
  protected enviar(): void {
    this.form.markAllAsTouched();
    this.form.controls.quantidade.updateValueAndValidity(); this.form.controls.dataEfetiva.updateValueAndValidity();
    const acao = this.acao(), v = this.form.getRawValue(), quantidade = normalizeDecimal(v.quantidade);
    if (this.bloqueado() || this.form.invalid || !acao || !quantidade || !idCustodia(v.corretoraDestinoId)
        || !['TRANSFERENCIA', 'ATRIBUICAO'].includes(v.natureza)) return;
    const base = { ticker: acao.ticker, mercado: acao.mercado, corretoraDestinoId: v.corretoraDestinoId, quantidade, dataEfetiva: v.dataEfetiva };
    let request: CadastroCustodia;
    if (v.natureza === 'TRANSFERENCIA') {
      if (!idCustodia(v.corretoraOrigemId)) return;
      request = { ...base, natureza: 'TRANSFERENCIA', corretoraOrigemId: v.corretoraOrigemId };
    } else request = { ...base, natureza: 'ATRIBUICAO', corretoraOrigemId: null };
    this.enviando.set(true); this.dialog.disableClose = true; this.erro.set(null);
    this.form.disable({ emitEvent: false });
    this.service.cadastrar(this.carteira.id, request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: registro => {
        if (this.encerrado) return;
        if (registro.carteiraId !== this.carteira.id) { this.falhou(normalizeHttpError(new HttpErrorResponse({ status: 0 }))); return; }
        this.dialog.close(registro);
      }, error: (erro: NormalizedHttpError) => { if (!this.encerrado) this.falhou(erro); }
    });
  }
  private falhou(erro: NormalizedHttpError): void {
    this.erro.set(erro); this.enviando.set(false); this.dialog.disableClose = false;
    this.form.enable({ emitEvent: false }); this.origemPorNatureza();
  }
  protected detalhesErro(): Record<string, unknown> { return detalhesQuantitativos(this.erro()); }
  protected mensagemErro(): string {
    const erro = this.erro();
    return erro?.status === 404 ? 'A carteira, ação ou corretora não está mais disponível. Confira as referências.'
      : mensagemQuantitativa(erro, 'cadastro') ?? (erro?.code === 'REQUEST_INVALIDO' ? 'Confira os campos informados.'
        : erro?.message ?? 'Não foi possível registrar a custódia.');
  }
}
