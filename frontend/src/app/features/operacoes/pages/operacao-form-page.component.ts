import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { catchError, combineLatest, distinctUntilChanged, finalize, map, of, startWith, switchMap, tap } from 'rxjs';

import { NormalizedHttpError } from '../../../core/errors/normalized-http-error';
import { FeedbackAlertComponent } from '../../../shared/feedback-alert/feedback-alert.component';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';
import { StickyBackComponent } from '../../../shared/sticky-back/sticky-back.component';
import { SuccessToastService } from '../../../shared/success-toast/success-toast.service';
import { AcaoResponse, Mercado } from '../../acoes/models/acao';
import { AcoesService } from '../../acoes/acoes.service';
import { CarteiraResponse } from '../../carteiras/models/carteira';
import { CarteirasService } from '../../carteiras/carteiras.service';
import { Corretora } from '../../corretoras/models/corretora';
import { CorretorasService } from '../../corretoras/corretoras.service';
import { OperacaoCreateRequest, OperacaoResponse, TipoOperacao } from '../models/operacao';
import { civilDateValidator, normalizeDecimal, positiveDecimalValidator, quantityValidator } from '../operacao-validators';
import { OperacoesService } from '../operacoes.service';

export interface OperacaoFormDialogData { carteira: CarteiraResponse; }

@Component({
  selector: 'app-operacao-form-page',
  imports: [FeedbackAlertComponent, MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule, PageHeaderComponent, ReactiveFormsModule, RouterLink, StickyBackComponent],
  template: `
    <section class="app-page" [class.app-dialog-page]="isDialog" aria-labelledby="operacao-form-title" aria-describedby="operacao-form-description">
      @if (!isDialog) { <app-sticky-back route="/operacoes" label="Voltar para operações" /> }
      <app-page-header headingId="operacao-form-title" eyebrow="Operações" title="Nova operação" [description]="contextPortfolio() ? 'Registre a movimentação na carteira ' + contextPortfolio()!.nome + '.' : 'Registre uma compra ou venda com os dados efetivamente negociados.'" />
      <p id="operacao-form-description" class="sr-only">Formulário de cadastro de operação financeira.</p>
      @if (error()) { <app-feedback-alert variant="error" [message]="errorMessage()" [details]="error()!.details" /> }
      @if (referencesLoading()) { <div class="app-state" role="status" aria-live="polite"><mat-spinner diameter="36" /> Carregando opções…</div> }
      @else {
        @if (referenceError()) { <app-feedback-alert variant="error" [message]="referenceError()!.message" [details]="referenceError()!.details" /> }
        @if (!contextPortfolio() && carteiras().length === 0) { <app-feedback-alert variant="warning" message="Cadastre uma carteira antes de registrar uma operação." /><a mat-stroked-button routerLink="/carteiras">Ir para carteiras</a> }
        @if (acoes().length === 0) { <app-feedback-alert variant="warning" message="Cadastre uma ação antes de registrar uma operação." /><a mat-stroked-button routerLink="/acoes">Ir para ações</a> }
        <form class="operation-form app-form-surface app-surface" [formGroup]="form" (ngSubmit)="submit()" novalidate>
          @if (contextPortfolio(); as carteira) { <div class="context"><strong>Carteira</strong><span>{{ carteira.nome }}</span><small>Definida pelo contexto e não editável.</small></div> }
          @else { <mat-form-field appearance="outline"><mat-label>Carteira</mat-label><mat-select formControlName="carteiraId">@for(item of carteiras();track item.id){<mat-option [value]="item.id">{{item.nome}}</mat-option>}</mat-select>@if(touchedInvalid('carteiraId')){<mat-error>Selecione uma carteira.</mat-error>}</mat-form-field> }
          <mat-form-field appearance="outline"><mat-label>Ação</mat-label><mat-select formControlName="acaoKey" (selectionChange)="marketChanged()">@for(item of acoes();track item.id){<mat-option [value]="actionKey(item)">{{item.ticker}} · {{item.mercado}}</mat-option>}</mat-select>@if(touchedInvalid('acaoKey')){<mat-error>Selecione uma ação.</mat-error>}</mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Corretora</mat-label><mat-select formControlName="corretoraId"><mat-option [value]="null">Sem corretora</mat-option>@for(item of corretoras();track item.id){<mat-option [value]="item.id">{{brokerName(item)}}</mat-option>}</mat-select><mat-hint>A corretora é opcional.</mat-hint></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Tipo</mat-label><mat-select formControlName="tipo"><mat-option value="COMPRA">Compra</mat-option><mat-option value="VENDA">Venda</mat-option></mat-select>@if(touchedInvalid('tipo')){<mat-error>Selecione COMPRA ou VENDA.</mat-error>}</mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Quantidade</mat-label><input matInput inputmode="decimal" formControlName="quantidade" aria-describedby="quantity-hint"/><mat-hint id="quantity-hint">{{ selectedMarket() === 'BRASIL' ? 'Brasil: somente unidades inteiras.' : 'EUA: até 6 casas decimais.' }}</mat-hint>@if(touchedInvalid('quantidade')){<mat-error>{{quantityError()}}</mat-error>}</mat-form-field>
          @if(form.controls.tipo.value){<mat-form-field appearance="outline"><mat-label>Preço unitário</mat-label>@if(priceCurrency()){<span matTextPrefix>{{ priceCurrency() === 'BRL' ? 'R$' : 'US$' }}&nbsp;</span>}<input matInput inputmode="decimal" formControlName="precoUnitario" [readonly]="form.controls.tipo.value === 'COMPRA'" [attr.aria-readonly]="form.controls.tipo.value === 'COMPRA'" aria-describedby="price-hint price-status"/><mat-hint id="price-hint">{{form.controls.tipo.value === 'COMPRA' ? 'Fechamento histórico exato, somente informativo.' : 'Valor editável; até 13 inteiros e 6 decimais.'}}</mat-hint>@if(touchedInvalid('precoUnitario')){<mat-error>Informe um preço positivo com até 13 inteiros e 6 decimais.</mat-error>}</mat-form-field>}
          <mat-form-field appearance="outline"><mat-label>Data da operação</mat-label><input matInput type="date" formControlName="dataOperacao"/>@if(touchedInvalid('dataOperacao')){<mat-error>Informe uma data válida, não futura no mercado.</mat-error>}</mat-form-field>
          <div id="price-status" class="price-status" aria-live="polite">@if(priceLoading()){<span role="status">Consultando preço…</span>}@if(priceError()){<app-feedback-alert variant="error" [message]="priceErrorMessage()" [details]="priceError()!.details" />}@if(form.controls.tipo.value === 'COMPRA' && previewReady()){<span>Fechamento de {{previewDate()}} em {{priceCurrency()}}. O backend confirmará o valor ao registrar.</span>}</div>
          @if (corretoras().length === 0) { <p class="optional-note">Nenhuma corretora cadastrada. Você pode continuar sem corretora.</p> }
          <div class="app-actions app-actions--stack-compact"><button mat-flat-button type="submit" [disabled]="submitBlocked()" [attr.aria-busy]="submitting()">Registrar operação</button>@if(isDialog){<button mat-button type="button" (click)="cancel()">Cancelar</button>}@else{<a mat-button routerLink="/operacoes">Cancelar</a>}</div>
          @if(submitting()){<div class="progress" role="status" aria-live="polite"><mat-spinner diameter="28"/> Registrando operação…</div>}
        </form>
      }
    </section>`,
  styles: [`
    .operation-form{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1.1rem;max-width:56rem}.operation-form>*{min-width:0}.context,.optional-note,.price-status,.app-actions,.progress{grid-column:1/-1}.context{display:grid;gap:.25rem;padding:1rem;border-radius:.75rem;background:var(--app-surface-selected)}.context small,.optional-note,.price-status{color:var(--app-text-secondary)}.price-status:empty{display:none}.progress{display:flex;align-items:center;gap:.75rem}.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0)}@media(max-width:42rem){.operation-form{grid-template-columns:1fr}.context,.optional-note,.price-status,.app-actions,.progress{grid-column:auto}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperacaoFormPageComponent {
  private readonly service = inject(OperacoesService);
  private readonly carteiraService = inject(CarteirasService);
  private readonly acaoService = inject(AcoesService);
  private readonly corretoraService = inject(CorretorasService);
  private readonly router = inject(Router);
  private readonly toast = inject(SuccessToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogRef = inject(MatDialogRef<OperacaoFormPageComponent, OperacaoResponse | undefined>, { optional: true });
  private readonly dialogData = inject<OperacaoFormDialogData | null>(MAT_DIALOG_DATA, { optional: true });
  protected readonly isDialog = this.dialogRef !== null;
  protected readonly contextPortfolio = signal(this.dialogData?.carteira ?? null);
  protected readonly carteiras = signal<CarteiraResponse[]>([]);
  protected readonly acoes = signal<AcaoResponse[]>([]);
  protected readonly corretoras = signal<Corretora[]>([]);
  protected readonly referencesLoading = signal(true);
  protected readonly referenceError = signal<NormalizedHttpError | null>(null);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly priceError = signal<NormalizedHttpError | null>(null);
  protected readonly priceLoading = signal(false);
  protected readonly previewReady = signal(false);
  protected readonly priceCurrency = signal<'BRL' | 'USD' | null>(null);
  protected readonly previewDate = signal('');
  protected readonly submitting = signal(false);
  private manualPriceVersion = 0;
  protected readonly form = new FormGroup({
    carteiraId: new FormControl<number | null>(this.dialogData?.carteira.id ?? null, [Validators.required]),
    acaoKey: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    corretoraId: new FormControl<number | null>(null),
    tipo: new FormControl<TipoOperacao | null>(null, [Validators.required]),
    quantidade: new FormControl('', { nonNullable: true, validators: [Validators.required, quantityValidator(() => this.selectedMarket())] }),
    precoUnitario: new FormControl('', { nonNullable: true }),
    dataOperacao: new FormControl('', { nonNullable: true, validators: [Validators.required, civilDateValidator(() => this.selectedMarket())] })
  });

  constructor() {
    this.form.controls.tipo.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.typeChanged());
    this.form.controls.precoUnitario.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.form.controls.tipo.value === 'VENDA') this.manualPriceVersion++;
    });
    this.setupPricePipeline();
    this.loadReferences();
  }

  private setupPricePipeline(): void {
    combineLatest([
      this.form.controls.tipo.valueChanges.pipe(startWith(this.form.controls.tipo.value)),
      this.form.controls.carteiraId.valueChanges.pipe(startWith(this.form.controls.carteiraId.value)),
      this.form.controls.acaoKey.valueChanges.pipe(startWith(this.form.controls.acaoKey.value)),
      this.form.controls.dataOperacao.valueChanges.pipe(startWith(this.form.controls.dataOperacao.value))
    ]).pipe(
      map(([tipo, carteiraId, acaoKey, dataOperacao]) => ({ tipo, carteiraId: this.contextPortfolio()?.id ?? carteiraId, acaoKey, dataOperacao })),
      distinctUntilChanged((a, b) => a.tipo === b.tipo && a.carteiraId === b.carteiraId && a.acaoKey === b.acaoKey && a.dataOperacao === b.dataOperacao),
      tap(() => this.invalidatePrice()),
      switchMap(context => {
        const action = this.acoes().find(item => this.actionKey(item) === context.acaoKey);
        if (!context.tipo || !action || !context.dataOperacao || this.form.controls.dataOperacao.invalid) return of(null);
        this.priceLoading.set(true);
        const editVersion = this.manualPriceVersion;
        if (context.tipo === 'COMPRA') {
          return this.service.obterPreviaCompra(action.ticker, action.mercado, context.dataOperacao).pipe(
            map(value => ({ kind: 'COMPRA' as const, value })),
            catchError((error: NormalizedHttpError) => { this.priceError.set(error); return of(null); }),
            finalize(() => this.priceLoading.set(false))
          );
        }
        if (!context.carteiraId) { this.priceLoading.set(false); return of(null); }
        return this.service.obterSugestaoPrecoVenda(context.carteiraId, action.ticker, action.mercado, context.dataOperacao).pipe(
          map(value => ({ kind: 'VENDA' as const, value, editVersion })),
          catchError((error: NormalizedHttpError) => { this.priceError.set(error); return of(null); }),
          finalize(() => this.priceLoading.set(false))
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(result => {
      if (!result?.value) return;
      if (result.kind === 'COMPRA') {
        this.form.controls.precoUnitario.setValue(result.value.precoUnitario, { emitEvent: false });
        this.priceCurrency.set(result.value.moeda);
        this.previewDate.set(result.value.dataCotacao);
        this.previewReady.set(true);
      } else if (result.editVersion === this.manualPriceVersion) {
        this.form.controls.precoUnitario.setValue(result.value.precoUnitarioSugerido ?? '', { emitEvent: false });
      }
    });
  }

  private invalidatePrice(): void {
    this.form.controls.precoUnitario.setValue('', { emitEvent: false });
    this.priceError.set(null);
    this.priceLoading.set(false);
    this.previewReady.set(false);
    this.priceCurrency.set(null);
    this.previewDate.set('');
  }

  private loadReferences(): void {
    let pending = this.contextPortfolio() ? 2 : 3;
    const done = () => { if (--pending === 0) this.referencesLoading.set(false); };
    if (!this.contextPortfolio()) this.carteiraService.listar().pipe(finalize(done), takeUntilDestroyed(this.destroyRef)).subscribe({ next: value => this.carteiras.set(value), error: error => this.referenceError.set(error) });
    this.acaoService.listar().pipe(finalize(done), takeUntilDestroyed(this.destroyRef)).subscribe({ next: value => this.acoes.set(value), error: error => this.referenceError.set(error) });
    this.corretoraService.listar().pipe(finalize(done), takeUntilDestroyed(this.destroyRef)).subscribe({ next: value => this.corretoras.set(value), error: error => this.referenceError.set(error) });
  }

  protected actionKey(item: AcaoResponse): string { return `${item.ticker}|${item.mercado}`; }
  protected selectedAction(): AcaoResponse | null { return this.acoes().find(action => this.actionKey(action) === this.form.controls.acaoKey.value) ?? null; }
  protected selectedMarket(): Mercado | null { return this.selectedAction()?.mercado ?? null; }
  protected marketChanged(): void { this.form.controls.quantidade.updateValueAndValidity(); this.form.controls.dataOperacao.updateValueAndValidity(); }
  protected typeChanged(): void {
    const price = this.form.controls.precoUnitario;
    price.setValue('', { emitEvent: false });
    if (this.form.controls.tipo.value === 'VENDA') price.setValidators([Validators.required, positiveDecimalValidator()]);
    else price.clearValidators();
    price.updateValueAndValidity();
  }
  protected brokerName(item: Corretora): string { return item.nomeFantasia || item.razaoSocial; }
  protected touchedInvalid(name: keyof typeof this.form.controls): boolean { const control = this.form.controls[name]; return control.touched && control.invalid; }
  protected quantityError(): string { return this.form.controls.quantidade.hasError('brazilianInteger') ? 'Ações brasileiras exigem quantidade inteira.' : 'Informe decimal positivo com até 13 inteiros e 6 decimais.'; }
  protected submitBlocked(): boolean { return this.submitting() || !!this.referenceError() || this.acoes().length === 0 || (!this.contextPortfolio() && this.carteiras().length === 0) || (this.form.controls.tipo.value === 'COMPRA' && !this.previewReady()); }
  protected priceErrorMessage(): string { return this.messageForError(this.priceError()!); }
  protected errorMessage(): string {
    return this.messageForError(this.error()!);
  }
  private messageForError(current: NormalizedHttpError): string {
    const guidance: Record<string, string> = {
      POSICAO_INSUFICIENTE: 'A quantidade excede a posição disponível nesse ponto cronológico.',
      COTACAO_HISTORICA_INDISPONIVEL: 'Não foi encontrado fechamento para a data informada. Escolha uma data em que tenha ocorrido pregão.',
      HISTORICO_COTACAO_FORA_DO_ALCANCE: 'A data informada está fora do histórico disponível para consulta.',
      TICKER_INEXISTENTE: 'O ticker informado não foi encontrado pelo provedor de mercado.',
      LIMITE_REQUISICOES_EXCEDIDO: 'O provedor de cotações atingiu temporariamente o limite de requisições. Tente novamente mais tarde.'
    };
    const context = current.code ? guidance[current.code] : undefined;
    return context ? `${context} ${current.message}` : current.message;
  }

  protected submit(): void {
    this.marketChanged();
    this.form.markAllAsTouched();
    const action = this.selectedAction();
    const tipo = this.form.controls.tipo.value;
    if (this.form.invalid || this.submitBlocked() || !action || !tipo) return;
    const quantidade = normalizeDecimal(this.form.controls.quantidade.value);
    if (!quantidade) return;
    const common = {
      carteiraId: this.contextPortfolio()?.id ?? this.form.controls.carteiraId.value!,
      ticker: action.ticker,
      mercado: action.mercado,
      corretoraId: this.form.controls.corretoraId.value,
      quantidade,
      dataOperacao: this.form.controls.dataOperacao.value
    };
    let request: OperacaoCreateRequest;
    if (tipo === 'COMPRA') request = { ...common, tipo };
    else {
      const precoUnitario = normalizeDecimal(this.form.controls.precoUnitario.value);
      if (!precoUnitario) return;
      request = { ...common, tipo, precoUnitario };
    }
    this.submitting.set(true);
    this.error.set(null);
    this.service.cadastrar(request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: item => this.complete(item),
      error: (error: NormalizedHttpError) => { this.error.set(error); this.submitting.set(false); }
    });
  }

  private complete(item: OperacaoResponse): void {
    if (this.dialogRef) { this.dialogRef.close(item); this.submitting.set(false); return; }
    void this.router.navigate(['/operacoes', item.id], { info: { operacao: item, origin: '/operacoes' } })
      .then(ok => { if (ok) this.toast.show('Operação registrada com sucesso.'); })
      .finally(() => this.submitting.set(false));
  }
  protected cancel(): void { this.dialogRef?.close(); }
}
