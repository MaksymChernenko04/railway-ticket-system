import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import PersonAddAlt1Icon from "@mui/icons-material/PersonAddAlt1";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { useI18n } from "../i18n/I18nContext.jsx";

const emptyForm = {
  firstName: "",
  lastName: "",
  benefitCode: "",
};

export function PassengerManager({
  passengers,
  benefits,
  isLoading,
  error,
  onCreate,
  onDelete,
  title = "Passengers",
  description = "Create passenger profiles and assign benefits when applicable.",
}) {
  const { codeLabel, t } = useI18n();
  const [form, setForm] = useState(emptyForm);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await onCreate({
        ...form,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
      });
      setForm(emptyForm);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5">{t(title)}</Typography>
        {description ? (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
            {t(description)}
          </Typography>
        ) : null}
      </Box>

      {error ? <Alert severity="error">{error}</Alert> : null}

      <Stack spacing={1.25}>
        {passengers.map((passenger) => (
          <Card key={passenger.id} variant="outlined">
            <CardContent sx={{ p: 1.75, "&:last-child": { pb: 1.75 } }}>
              <Stack direction="row" spacing={1.5} alignItems="center" justifyContent="space-between">
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                    {passenger.firstName} {passenger.lastName}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {passenger.benefitCode
                      ? t("{code} - {percent}% discount", {
                          code: codeLabel("benefit", passenger.benefitCode),
                          percent: passenger.benefitPercent,
                        })
                      : t("No benefit")}
                  </Typography>
                </Box>
                <IconButton
                  color="error"
                  onClick={() => onDelete(passenger.id)}
                  disabled={isLoading || isSubmitting}
                  aria-label={t("Delete")}
                >
                  <DeleteOutlineIcon />
                </IconButton>
              </Stack>
            </CardContent>
          </Card>
        ))}
      </Stack>

      {!passengers.length && !isLoading ? <Alert severity="info">{t("No passengers yet.")}</Alert> : null}

      <Box component="form" onSubmit={handleSubmit}>
        <Stack spacing={1.5}>
          <TextField
            label={t("First name")}
            value={form.firstName}
            onChange={(event) => setForm((current) => ({ ...current, firstName: event.target.value }))}
            required
            fullWidth
          />
          <TextField
            label={t("Last name")}
            value={form.lastName}
            onChange={(event) => setForm((current) => ({ ...current, lastName: event.target.value }))}
            required
            fullWidth
          />
          <FormControl fullWidth disabled={!benefits.length}>
            <InputLabel id="passenger-benefit-label">{t("Benefit")}</InputLabel>
            <Select
              labelId="passenger-benefit-label"
              label={t("Benefit")}
              value={form.benefitCode}
              onChange={(event) => setForm((current) => ({ ...current, benefitCode: event.target.value }))}
            >
              <MenuItem value="">
                {t("No benefit")}
              </MenuItem>
              {benefits.map((benefit) => (
                <MenuItem key={benefit.code} value={benefit.code}>
                  {codeLabel("benefit", benefit.code)} - {benefit.percentAmount}%
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button
            type="submit"
            variant="outlined"
            startIcon={<PersonAddAlt1Icon />}
            disabled={isLoading || isSubmitting}
          >
            {isSubmitting ? t("Adding...") : t("Add passenger")}
          </Button>
        </Stack>
      </Box>
    </Stack>
  );
}
