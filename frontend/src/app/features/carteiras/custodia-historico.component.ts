import { afterNextRender, ChangeDetectionStrategy, Component, DestroyRef, effect, ElementRef, inject, Injector, input, output, signal, untracked, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { normalizeHttpError } from '../../core/http/http-error-normalizer';
import { FeedbackAlertComponent } from '../../shared/feedback-alert/feedback-alert.component';
import { formatOffsetDateTime } from '../../shared/formatters/offset-date-time.formatter';
import { formatCivilDate, formatOperationQuantity } from '../operacoes/operacao-validators';
import { CorretoraNamesService } from '../corretoras/corretora-names.service';
import { CustodiaService } from './custodia.service';
import { DetalheCustodia, FiltrosCustodia, MovimentacaoCustodia, RegistroCustodia } from './models/custodia';
import { CustodiaEstornoDialogComponent } from './custodia-estorno-dialog.component';
import { vinculoCustodia } from './custodia.contract';

@Component({ selector: 'app-custodia-historico',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatSelectModule, FeedbackAlertComponent],
  templateUrl: './custodia-historico.component.html',
  styleUrls: ['../../shared/collection/collection.scss', '../../shared/form/simple-form.scss'],
  styles: ['.filtros{display:flex;flex-wrap:wrap;gap:1rem;align-items:center}mat-form-field{flex:1 1 12rem}.posicao{overflow-wrap:anywhere}'],
  changeDetection: ChangeDetectionStrategy.OnPush })
export class CustodiaHistoricoComponent {
  readonly carteiraId = input.required<number>();
  readonly refreshVersion = input(0);
  readonly estornoConfirmado = output<DetalheCustodia>();
  private readonly service = inject(CustodiaService);
  private readonly names = inject(CorretoraNamesService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly tituloHistorico = viewChild.required<ElementRef<HTMLHeadingElement>>('tituloHistorico');
  private aberto?: MatDialogRef<CustodiaEstornoDialogComponent, DetalheCustodia>;
  private contexto = 0;
  private carteiraAnterior?: number;
  private revisao = 0;
  private readonly confirmados = signal<ReadonlySet<number>>(new Set());
  protected readonly consultaAnteriorAoEstorno = signal(false);
  private request?: Subscription;
  private geracao = 0;
  protected readonly itens = signal<RegistroCustodia[]>([]);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<NormalizedHttpError | null>(null);
  protected readonly filtros = new FormGroup({ natureza: new FormControl<FiltrosCustodia['natureza'] | ''>('', { nonNullable: true }),
    situacao: new FormControl<FiltrosCustodia['situacao'] | ''>('', { nonNullable: true }) });
  protected readonly quantidade = formatOperationQuantity;
  protected readonly dataCivil = formatCivilDate;
  protected readonly instante = formatOffsetDateTime;
  constructor() {
    effect(() => { const id = this.carteiraId(); this.refreshVersion(); untracked(() => {
      if (id !== this.carteiraAnterior) {
        this.carteiraAnterior = id; this.contexto++; this.aberto?.close(); this.aberto = undefined;
        this.confirmados.set(new Set()); this.revisao = 0; this.consultaAnteriorAoEstorno.set(false);
        this.filtros.reset({ natureza: '', situacao: '' }, { emitEvent: false });
      }
      this.carregar(id);
    }); });
    this.filtros.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.carregar(this.carteiraId()));
    this.destroyRef.onDestroy(() => { this.contexto++; this.geracao++; this.request?.unsubscribe(); this.aberto?.close(); });
  }
  protected nome(id: number | null): string { return id === null ? 'Sem corretora informada' : this.names.name(id); }
  protected identidade(r: RegistroCustodia): string { return `${r.tipoRegistro}:${r.id}`; }
  protected originalId(r: RegistroCustodia): number { return r.tipoRegistro === 'MOVIMENTACAO' ? r.id : r.movimentacaoOriginalId; }
  protected podeEstornar(r: RegistroCustodia): r is MovimentacaoCustodia {
    return r.tipoRegistro === 'MOVIMENTACAO' && r.situacao === 'ATIVA' && !this.confirmados().has(r.id);
  }
  protected confirmarEstorno(r: RegistroCustodia): void {
    const carteiraId = this.carteiraId(), contexto = this.contexto;
    if (!this.podeEstornar(r) || r.carteiraId !== carteiraId || this.aberto) return;
    const acionador = this.host.nativeElement.querySelector<HTMLButtonElement>(`[data-estornar="${r.id}"]`);
    const ref = this.dialog.open<CustodiaEstornoDialogComponent, { carteiraId: number; movimentacao: MovimentacaoCustodia }, DetalheCustodia>(
      CustodiaEstornoDialogComponent, { data: { carteiraId, movimentacao: r }, panelClass: ['app-create-dialog', 'app-dialog--form'],
        autoFocus: 'first-tabbable', restoreFocus: true, ariaLabelledBy: 'custodia-estorno-title', ariaDescribedBy: 'custodia-estorno-description' });
    this.aberto = ref;
    ref.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(detalhe => {
      if (this.aberto === ref) this.aberto = undefined;
      if (contexto !== this.contexto || carteiraId !== this.carteiraId()) return;
      // Material restaura o acionador antes de afterClosed; a recarga pode removê-lo depois.
      afterNextRender(() => {
        if (this.destroyRef.destroyed || contexto !== this.contexto || carteiraId !== this.carteiraId() || this.aberto) return;
        const destino = acionador?.isConnected ? acionador : this.tituloHistorico().nativeElement;
        if (destino.isConnected) destino.focus();
      }, { injector: this.injector });
      if (!detalhe || detalhe.movimentacao.id !== r.id || detalhe.movimentacao.carteiraId !== carteiraId
          || !detalhe.estorno || !vinculoCustodia(detalhe.movimentacao, detalhe.estorno) || this.confirmados().has(r.id)) return;
      this.confirmados.set(new Set([...this.confirmados(), r.id])); this.revisao++;
      // Mantém o snapshot recebido intacto; recarga coordenada pertence à tarefa 5.7.
      this.consultaAnteriorAoEstorno.set(true); this.estornoConfirmado.emit(detalhe);
    });
  }
  protected carregar(id = this.carteiraId()): void {
    const geracao = ++this.geracao;
    const revisao = this.revisao;
    this.request?.unsubscribe(); this.carregando.set(true); this.erro.set(null); this.itens.set([]);
    const v = this.filtros.getRawValue();
    const filtros: FiltrosCustodia = { ...(v.natureza ? { natureza: v.natureza } : {}), ...(v.situacao ? { situacao: v.situacao } : {}) };
    this.request = this.service.historico(id, filtros).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: itens => {
      if (geracao !== this.geracao || id !== this.carteiraId()) return;
      this.carregando.set(false);
      if (itens.some(r => r.carteiraId !== id)) { this.erro.set(normalizeHttpError(new HttpErrorResponse({ status: 0 }))); return; }
      this.itens.set(itens);
      if (revisao === this.revisao) this.consultaAnteriorAoEstorno.set(false);
      this.names.loadFor(itens.flatMap(r => [{ corretoraId: r.corretoraOrigemId }, { corretoraId: r.corretoraDestinoId }]));
    }, error: (erro: NormalizedHttpError) => {
      if (geracao !== this.geracao || id !== this.carteiraId()) return;
      this.carregando.set(false); this.erro.set(erro);
    } });
  }
}
