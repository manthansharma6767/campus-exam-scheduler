import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';

export default function ImportPreviewPanel({ preview, counts }) {
  if (!preview) return null;

  const rows = preview.rows || [];
  const columns = Object.keys(rows[0]?.values || {});
  const summaryCounts = counts || [
    { label: `${preview.validRows} valid row(s)`, color: 'success' },
    { label: `${preview.skippedDuplicates} duplicate(s) to skip` },
    { label: `${preview.errors?.length || 0} row error(s)`, color: preview.errors?.length ? 'error' : 'default' },
    { label: `${preview.conflicts?.length || 0} schedule conflict(s)`, color: preview.conflicts?.length ? 'error' : 'default' },
  ];

  return (
    <Box sx={{ mt: 2 }}>
      <Alert severity={preview.canImport ? 'success' : 'warning'} sx={{ mb: 1.5 }}>
        {preview.message}
      </Alert>
      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
        {summaryCounts.map((count) => (
          <Chip key={count.label} size="small" label={count.label} color={count.color || 'default'} variant="outlined" />
        ))}
      </Box>

      {!!preview.errors?.length && (
        <Box sx={{ mb: 2 }}>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Rows that need correction</Typography>
          {preview.errors.map((error, index) => (
            <Alert key={`${error.rowNumber}-${index}`} severity="error" sx={{ mb: 0.5, py: 0 }}>
              Row {error.rowNumber}: {error.message}
            </Alert>
          ))}
        </Box>
      )}

      {!!preview.conflicts?.length && (
        <Box sx={{ mb: 2 }}>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Schedule conflicts</Typography>
          {preview.conflicts.map((conflict, index) => (
            <Alert key={`${conflict.rowNumber}-${conflict.type}-${index}`} severity="error" sx={{ mb: 0.5, py: 0 }}>
              {conflict.rowNumber > 0 ? `Row ${conflict.rowNumber}: ` : ''}{conflict.message}
            </Alert>
          ))}
        </Box>
      )}

      {!!rows.length && (
        <>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Import preview</Typography>
          {preview.previewTruncated && (
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5 }}>
              Showing the first {rows.length} preview rows.
            </Typography>
          )}
          <TableContainer sx={{ maxHeight: 280, border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>File row</TableCell>
                  {columns.map((column) => <TableCell key={column}>{column}</TableCell>)}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.rowNumber} hover>
                    <TableCell>{row.rowNumber}</TableCell>
                    {columns.map((column) => <TableCell key={column}>{row.values?.[column] || ''}</TableCell>)}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </Box>
  );
}
