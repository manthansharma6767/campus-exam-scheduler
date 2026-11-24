function escapeCsvValue(value) {
  const text = value == null ? '' : String(value);
  return `"${text.replace(/"/g, '""')}"`;
}

export function buildCsv(headers, rows) {
  return [headers, ...rows]
    .map((row) => row.map(escapeCsvValue).join(','))
    .join('\r\n');
}

export function downloadCsv(filename, headers, rows) {
  const blob = new Blob([`\uFEFF${buildCsv(headers, rows)}`], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export function slugifyFilename(value, fallback = 'export') {
  const slug = String(value || '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return slug || fallback;
}
