import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { HttpErrorResponse } from '@angular/common/http';
import { DetalheCustodia, MovimentacaoCustodia } from './models/custodia';
import { CustodiaService } from './custodia.service';
import { isRegistroCustodia, vinculoCustodia } from './custodia.contract';
import { FeedbackAlertComponent } from '../../shared/feedback-alert/feedback-alert.component';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { detalhesQuantitativos, mensagemQuantitativa } from '../../core/errors/quantitative-errors';
import { normalizeHttpError } from '../../core/http/http-error-normalizer';
import { formatCivilDate, formatOperationQuantity } from '../operacoes/operacao-validators';

export interface CustodiaEstornoDialogData { carteiraId: number; movimentacao: MovimentacaoCustodia }
@Component({ selector: 'app-custodia-estorno-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, FeedbackAlertComponent],
  templateUrl: './custodia-estorno-dialog.component.html',
  styleUrls: ['../../shared/form/simple-form.scss', '../../shared/dialog/dialog.scss'],
  styles: [':host{display:block;min-width:0}.original{overflow-wrap:anywhere}'],
  changeDetection: ChangeDetectionStrategy.OnPush })
export class CustodiaEstornoDialogComponent {
  private readonly data = inject<CustodiaEstornoDialogData>(MAT_DIALOG_DATA);
  private readonly carteiraId = this.data.carteiraId;
  protected readonly original = Object.freeze({ ...this.data.movimentacao,
    posicaoCronologica: Object.freeze({ ...this.data.movimentacao.posicaoCronologica }) });
  private readonly dialog = inject<MatDialogRef<CustodiaEstornoDialogComponent, DetalheCustodia>>(MatDialogRef);
  private readonly service = inject(CustodiaService);
  private readonly destroyRef = inject(DestroyRef);
  private encerrado = false;
  protected readonly enviando = signal(false);
  protected readonly erro = signal<NormalizedHttpError | null>(null);
  protected readonly form = new FormGroup({ motivo: new FormControl('', { nonNullable: true, validators: Validators.maxLength(1000) }) });
  protected readonly quantidade = formatOperationQuantity;
  protected readonly dataCivil = formatCivilDate;
  constructor() { this.destroyRef.onDestroy(() => { this.encerrado = true; }); }
  protected elegivel(): boolean {
    return isRegistroCustodia(this.original, this.carteiraId) && this.original.tipoRegistro === 'MOVIMENTACAO' && this.original.situacao === 'ATIVA';
  }
  protected cancelar(): void { if (!this.enviando()) this.dialog.close(); }
  protected confirmar(): void {
    this.form.markAllAsTouched();
    if (this.encerrado || this.enviando() || this.form.invalid || !this.elegivel()) return;
    const motivo = this.form.controls.motivo.value;
    this.enviando.set(true); this.erro.set(null); this.dialog.disableClose = true; this.form.disable({ emitEvent: false });
    const request = motivo === '' ? this.service.estornar(this.carteiraId, this.original.id)
      : this.service.estornar(this.carteiraId, this.original.id, { motivo });
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: detalhe => {
      if (this.encerrado) return;
      if (detalhe.movimentacao.carteiraId !== this.carteiraId || detalhe.movimentacao.id !== this.original.id
          || !detalhe.estorno || !vinculoCustodia(detalhe.movimentacao, detalhe.estorno)) {
        this.falhou(normalizeHttpError(new HttpErrorResponse({ status: 0 }))); return;
      }
      this.encerrado = true; this.dialog.close(detalhe);
    }, error: (erro: NormalizedHttpError) => { if (!this.encerrado) this.falhou(erro); } });
  }
  private falhou(erro: NormalizedHttpError): void {
    this.erro.set(erro); this.enviando.set(false); this.dialog.disableClose = false; this.form.enable({ emitEvent: false });
  }
  protected detalhesErro(): Record<string, unknown> { return detalhesQuantitativos(this.erro()); }
  protected mensagemErro(): string {
    const erro = this.erro();
    return erro?.status === 404 ? 'Movimentação não encontrada nesta carteira.'
      : mensagemQuantitativa(erro, 'estorno') ?? (erro?.code === 'REQUEST_INVALIDO' ? 'Confira o motivo e os dados da movimentação.'
        : erro?.message ?? 'Não foi possível confirmar o estorno.');
  }
}
