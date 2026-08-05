import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import Box from '@mui/material/Box';

export default function ConfirmDialog({ open, title, message, onConfirm, onCancel }) {
  return (
    <Dialog
      open={open}
      onClose={onCancel}
      maxWidth="xs"
      fullWidth
      aria-labelledby="confirm-dialog-title"
      aria-describedby="confirm-dialog-description"
    >
      <DialogTitle id="confirm-dialog-title" sx={{ pb: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <WarningAmberIcon sx={{ color: '#FFB300' }} />
          {title || 'Confirm Action'}
        </Box>
      </DialogTitle>
      <DialogContent>
        <DialogContentText id="confirm-dialog-description" sx={{ color: '#B0B0B0' }}>
          {message || 'Are you sure you want to proceed? This action cannot be undone.'}
        </DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button
          onClick={onCancel}
          variant="outlined"
          color="inherit"
          aria-label="Cancel action"
          sx={{ borderColor: 'rgba(255,255,255,0.2)', color: '#B0B0B0' }}
        >
          Cancel
        </Button>
        <Button
          onClick={onConfirm}
          variant="contained"
          color="error"
          aria-label="Confirm action"
        >
          Delete
        </Button>
      </DialogActions>
    </Dialog>
  );
}
