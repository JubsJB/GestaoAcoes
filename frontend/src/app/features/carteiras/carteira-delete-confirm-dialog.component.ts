import { afterNextRender, ChangeDetectionStrategy, Component, computed, ElementRef, inject, Injector, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef, MatDialogState } from '@angular/material/dialog';

import { CarteiraContextService } from '../../core/carteira/carteira-context.service';
import { NormalizedHttpError } from '../../core/errors/normalized-http-error';
import { CarteirasService } from './carteiras.service';
import { CarteiraResponse } from './models/carteira';

const DELETION_BLOCKS = new Map([
  ['CARTEIRA_POSSUI_OPERACOES', { subtitle: 'Operações vinculadas', message: 'Esta carteira possui operações registradas e não pode ser excluída.' }],
  ['CARTEIRA_POSSUI_SNAPSHOTS', { subtitle: 'Snapshots vinculados', message: 'Esta carteira possui registros de evolução patrimonial (snapshots) e não pode ser excluída.' }],
  ['CARTEIRA_POSSUI_MOVIMENTACOES_CUSTODIA', { subtitle: 'Histórico de custódia', message: 'Esta carteira possui histórico de custódia e não pode ser excluída.' }]
]);

interface DeletionFeedback {
  title: string;
  subtitle: string;
  message: string;
  retryable: boolean;
  warning?: boolean;
}

@Component({
  selector: 'app-carteira-delete-confirm-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <header class="delete-heading">
      <span class="delete-icon" [class.delete-icon--warning]="feedback()?.warning" [class.delete-icon--info]="feedback() && !feedback()!.warning" aria-hidden="true">
        <svg viewBox="0 0 24 24" focusable="false" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          @if (!feedback()) { <path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7" /> }
          @else if (feedback()!.warning) { <path d="M12 3 22 21H2Z M12 9v5 M12 17v.1" /> }
          @else { <circle cx="12" cy="12" r="9" /><path d="M12 11v6 M12 7v.1" /> }
        </svg>
      </span>
      <div><h2 mat-dialog-title>{{ feedback()?.title ?? 'Excluir carteira' }}</h2><p class="delete-subtitle">{{ feedback()?.subtitle ?? 'Confirme a exclusão' }}</p></div>
    </header>
    <mat-dialog-content class="delete-content">
      <div role="alert" aria-atomic="true">@if (feedback(); as failure) { <p class="delete-question">{{ failure.message }}</p> }</div>
      @if (!feedback()) { <p class="delete-question">Deseja excluir esta carteira?</p> }
      <dl class="delete-target"><dt>Carteira</dt><dd>{{ data.nome }}</dd></dl>
    </mat-dialog-content>
    <mat-dialog-actions class="app-dialog-actions" align="end">
      @if (feedback() && !feedback()!.retryable) {
      <button #feedbackAction mat-flat-button type="button" (click)="cancel()">Entendi</button>
      } @else {
      <button #feedbackAction class="delete-cancel" mat-stroked-button type="button" [disabled]="deleting()" (click)="cancel()">Cancelar</button>
      <button class="app-destructive" mat-flat-button type="button" [disabled]="deleting()" [attr.aria-busy]="deleting()" (click)="confirm()">{{ feedback() ? 'Tentar excluir novamente' : 'Excluir carteira' }}</button>
      }
    </mat-dialog-actions>
  `,
  styleUrl: './carteira-delete-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CarteiraDeleteConfirmDialogComponent {
  protected readonly data = inject<CarteiraResponse>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<CarteiraDeleteConfirmDialogComponent, boolean>);
  private readonly service = inject(CarteirasService);
  private readonly context = inject(CarteiraContextService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly feedbackAction = viewChild<unknown, ElementRef<HTMLButtonElement>>('feedbackAction', { read: ElementRef });
  protected readonly deleting = signal(false);
  protected readonly error = signal<NormalizedHttpError | null>(null);
  protected readonly feedback = computed<DeletionFeedback | undefined>(() => {
    const error = this.error();
    if (!error) return undefined;
    if (error.kind === 'standard' && error.status === 409 && error.code) {
      const blocked = DELETION_BLOCKS.get(error.code);
      if (blocked) return { ...blocked, title: 'Não é possível excluir', retryable: false, warning: true };
      if (error.code === 'CONFLITO_TRANSACIONAL_TRANSITORIO') return {
        title: 'Não foi possível excluir', subtitle: 'Conflito temporário',
        message: 'Um conflito temporário impediu a exclusão. Você pode tentar novamente.', retryable: true
      };
    }
    if (error.kind === 'standard' && error.status === 403 && error.code === 'CSRF_INVALIDO') return {
      title: 'Não foi possível excluir', subtitle: 'Verificação de segurança', retryable: true,
      message: 'Não foi possível validar a solicitação. Tente novamente.'
    };
    if (error.status === 401) return {
      title: 'Sessão não confirmada', subtitle: 'Verifique seu acesso', retryable: false,
      message: 'Sua sessão precisa ser confirmada. Entre novamente para continuar.'
    };
    if (error.status === 403) return {
      title: 'Exclusão não autorizada', subtitle: 'Acesso não autorizado', retryable: false,
      message: 'A solicitação de exclusão não foi autorizada.'
    };
    if (error.kind === 'standard' && error.status === 503 && error.code === 'SEGURANCA_INDISPONIVEL') return {
      title: 'Verificação de sessão indisponível', subtitle: 'Verifique seu acesso', retryable: false,
      message: 'Não foi possível verificar sua sessão. Tente acessar novamente mais tarde.'
    };
    if (error.status === 404) return {
      title: 'Carteira não encontrada', subtitle: 'Atualize a consulta', retryable: false,
      message: 'Esta carteira não foi encontrada. Feche esta mensagem e atualize a consulta.'
    };
    if (error.status === 400) return {
      title: 'Solicitação não processada', subtitle: 'Verifique os dados', retryable: false,
      message: 'Não foi possível processar a solicitação de exclusão. Feche esta mensagem e verifique os dados.'
    };
    if (error.status === 409) return {
      title: 'Não foi possível excluir', subtitle: 'Verificação necessária', retryable: false,
      message: 'A exclusão não pôde ser concluída. Verifique o estado atual da carteira antes de tentar novamente.'
    };
    return {
      title: 'Verifique o estado da carteira', subtitle: 'Resultado não confirmado', retryable: false,
      message: 'Não foi possível confirmar o resultado da exclusão. Feche esta mensagem e verifique o estado atual da carteira antes de tentar novamente.'
    };
  });

  protected cancel(): void { if (!this.deleting()) this.dialogRef.close(false); }

  protected confirm(): void {
    if (this.deleting() || (this.feedback() && !this.feedback()!.retryable)) return;
    this.deleting.set(true);
    this.error.set(null);
    this.service.excluir(this.data.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.context.remove(this.data.id); this.dialogRef.close(true); },
      error: (error: NormalizedHttpError) => {
        this.error.set(error); this.deleting.set(false);
        afterNextRender(() => {
          if (!this.destroyRef.destroyed && this.dialogRef.getState() === MatDialogState.OPEN) {
            this.feedbackAction()?.nativeElement.focus();
          }
        }, { injector: this.injector });
      }
    });
  }
}
