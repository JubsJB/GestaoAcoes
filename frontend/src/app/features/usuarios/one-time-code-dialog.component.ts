import { Component, inject, OnDestroy, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { formatOffsetDateTime } from '../../shared/formatters/offset-date-time.formatter';
import { EmissaoAutorizacao } from './usuario.model';

@Component({
  selector: 'app-one-time-code-dialog', imports: [MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  styleUrl: '../../shared/dialog/dialog.scss',
  template: `<h2 mat-dialog-title>Código de {{ data.motivo === 'INICIAL' ? 'definição inicial' : 'redefinição' }}</h2>
    <mat-dialog-content><p>Usuário: <strong>{{ data.username }}</strong></p><p>Este código é exibido somente agora. Entregue-o ao usuário por um canal seguro. A aplicação não poderá recuperá-lo.</p>
    <p>Válido até {{ expiry }}. Um novo código invalida o anterior.</p>
    <mat-form-field style="width:100%"><mat-label>Código de uso único</mat-label><input matInput readonly data-testid="one-time-code" [value]="data.codigo" (focus)="select($event)"></mat-form-field>
    @if (message()) { <p role="status">{{ message() }}</p> }</mat-dialog-content>
    <mat-dialog-actions><button mat-button (click)="copy()">Copiar código</button><button mat-flat-button data-testid="close-code" mat-dialog-close>Concluir</button></mat-dialog-actions>`
})
export class OneTimeCodeDialogComponent implements OnDestroy {
  protected readonly data = inject<{ username: string } & EmissaoAutorizacao['autorizacao']>(MAT_DIALOG_DATA);
  protected readonly expiry = formatOffsetDateTime(this.data.expiraEm);
  protected readonly message = signal('');
  constructor() { inject(MatDialogRef).beforeClosed().subscribe(() => this.clear()); }
  protected select(event: Event): void { (event.target as HTMLInputElement).select(); }
  protected async copy(): Promise<void> {
    try { await navigator.clipboard.writeText(this.data.codigo); this.message.set('Código copiado. Limpe a área de transferência após a entrega.'); }
    catch { this.message.set('Não foi possível copiar. Selecione o código e copie manualmente.'); }
  }
  private clear(): void { this.data.codigo = ''; }
  ngOnDestroy(): void { this.clear(); }
}
