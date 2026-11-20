export function formatOffsetDateTime(value: string): string {
  // Date accepts ISO offsets only to minutes. Parse the civil clock as UTC,
  // then subtract the complete offset, including seconds. Keep the original
  // fraction/text untouched; the minute-only presentation never rounds it.
  const iso = /^([+-]?\d{4,})(-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d{1,9})?)?)(Z|([+-])(\d{2}):(\d{2})(?::(\d{2}))?)$/.exec(value);
  let date: Date;
  if (iso) {
    const year = Number(iso[1]);
    const runtimeYear = year >= 0 && year <= 9999
      ? String(year).padStart(4, '0')
      : `${year < 0 ? '-' : '+'}${String(Math.abs(year)).padStart(6, '0')}`;
    const offsetSeconds = iso[3] === 'Z' ? 0
      : (iso[4] === '-' ? -1 : 1) * (Number(iso[5]) * 3600 + Number(iso[6]) * 60 + Number(iso[7] ?? 0));
    date = new Date(new Date(`${runtimeYear}${iso[2]}Z`).getTime() - offsetSeconds * 1000);
  } else {
    date = new Date(value);
  }
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const parts = new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).formatToParts(date);
  const part = (type: Intl.DateTimeFormatPartTypes): string =>
    parts.find((item) => item.type === type)?.value ?? '';

  return `${part('day')}/${part('month')}/${part('year')} às ${part('hour')}:${part('minute')}`;
}
