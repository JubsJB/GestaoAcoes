import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

@Component({ host: { class: 'app-blue-scope' }, selector: 'app-admin-confirm-dialog', imports: [MatDialogModule, MatButtonModule], styleUrl: './usuarios-dialog.scss',
  template: `<h2 mat-dialog-title>{{ data.title }}</h2><mat-dialog-content><p>{{ data.message }}</p></mat-dialog-content><mat-dialog-actions><button mat-button mat-dialog-close>Cancelar</button><button mat-flat-button data-testid="confirm-admin" [mat-dialog-close]="true">Confirmar</button></mat-dialog-actions>` })
export class AdminConfirmDialogComponent { protected readonly data = inject<{ title: string; message: string }>(MAT_DIALOG_DATA); }
