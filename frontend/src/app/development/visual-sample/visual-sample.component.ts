import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { FeedbackAlertComponent } from '../../shared/feedback-alert/feedback-alert.component';
import { AppIconComponent } from '../../shared/app-icon/app-icon.component';

@Component({
  selector: 'app-root',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, PageHeaderComponent, FeedbackAlertComponent, AppIconComponent],
  templateUrl: './visual-sample.component.html',
  styleUrls: ['../../shared/collection/refined-collection.scss', './visual-sample.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VisualSampleComponent {
  protected readonly form = new FormGroup({ name: new FormControl('', { nonNullable: true, validators: Validators.required }) });
  protected readonly validated = signal(false);
  protected readonly rows = [
    { name: 'Carteira de longo prazo', context: 'Demonstração · Brasil', status: 'Ativo', value: 'R$ 24.580,00', result: '+R$ 1.240,00', outcome: 'Positivo' },
    { name: 'Reserva para novos objetivos', context: 'Demonstração · Brasil', status: 'Ativo', value: 'R$ 8.350,00', result: '−R$ 180,00', outcome: 'Negativo' },
    { name: 'Carteira em preparação', context: 'Demonstração · sem posições', status: 'Pendente', value: 'R$ 0,00', result: 'R$ 0,00', outcome: 'Neutro' }
  ];
  protected validate(): void {
    this.form.markAllAsTouched();
    this.validated.set(this.form.valid);
  }
}
