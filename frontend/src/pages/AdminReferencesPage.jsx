import CategoryIcon from "@mui/icons-material/Category";
import RefreshIcon from "@mui/icons-material/Refresh";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Container,
  Paper,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { nextSortState, SortableTableCell, sortRows } from "../components/SortableTableCell.jsx";
import { useI18n } from "../i18n/I18nContext.jsx";

const referenceTypes = [
  { type: "train-types", label: "Train types", valueField: "priceMultiplier", valueLabel: "Price multiplier" },
  { type: "car-types", label: "Car types", valueField: "priceMultiplier", valueLabel: "Price multiplier" },
  { type: "seat-types", label: "Seat types", valueField: "priceMultiplier", valueLabel: "Price multiplier" },
  { type: "currencies", label: "Currencies" },
  { type: "countries", label: "Countries" },
  { type: "benefits", label: "Benefits", valueField: "percentAmount", valueLabel: "Percent amount" },
];

const referenceCodeGroups = {
  "train-types": "trainType",
  "car-types": "carType",
  "seat-types": "seatType",
  benefits: "benefit",
};

const emptyForm = {
  code: "",
  value: "",
};

export function AdminReferencesPage() {
  const { authorizedGet, authorizedPost, authorizedDelete, user } = useAuth();
  const { t, codeLabel } = useI18n();
  const [activeType, setActiveType] = useState(referenceTypes[0].type);
  const [referencesPage, setReferencesPage] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pageIndex, setPageIndex] = useState(0);
  const [sort, setSort] = useState({ field: "code", direction: "asc" });

  const config = useMemo(
    () => referenceTypes.find((referenceType) => referenceType.type === activeType) ?? referenceTypes[0],
    [activeType],
  );
  const sortedReferences = useMemo(
    () => sortRows(referencesPage?.content ?? [], sort, config.valueField ? {
      [config.valueField]: (reference) => Number(reference[config.valueField]),
    } : {}),
    [referencesPage, sort, config],
  );

  async function loadReferences(page = pageIndex, type = activeType) {
    setIsLoading(true);
    setError("");

    try {
      const data = await authorizedGet(`/api/admin/references/${type}?page=${page}&size=8&sort=code,asc`);
      setReferencesPage(data);
      setPageIndex(page);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadReferences(0, activeType);
  }, []);

  async function handleTypeChange(nextType) {
    setActiveType(nextType);
    setFeedback("");
    setError("");
    setForm(emptyForm);
    setSort({ field: "code", direction: "asc" });
    await loadReferences(0, nextType);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setFeedback("");
    setError("");
    setIsSubmitting(true);

    const payload = { code: form.code };
    if (config.valueField) {
      payload[config.valueField] = Number(form.value);
    }

    try {
      await authorizedPost(`/api/admin/references/${activeType}`, payload);
      setFeedback(t("Reference created."));
      setForm(emptyForm);
      await loadReferences(0, activeType);
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function deleteReference(code) {
    if (!globalThis.confirm(t("Are you sure you want to delete {code}?", { code: formatReferenceCode(code) }))) {
      return;
    }

    setFeedback("");
    setError("");

    try {
      await authorizedDelete(`/api/admin/references/${activeType}/${encodeURIComponent(code)}`);
      setFeedback(t("Reference deleted."));

      const isLastItemOnPage = referencesPage?.content?.length === 1;
      const nextPageIndex = (isLastItemOnPage && pageIndex > 0) ? pageIndex - 1 : pageIndex;

      await loadReferences(nextPageIndex, activeType);
    } catch (deleteError) {
      setError(deleteError.message);
    }
  }

  function formatValue(reference) {
    if (!config.valueField) {
      return "-";
    }

    return reference[config.valueField];
  }

  function formatReferenceCode(code) {
    const group = referenceCodeGroups[activeType];
    return group ? codeLabel(group, code) : code;
  }

  function handleSort(field) {
    setSort((current) => nextSortState(current, field));
  }

  return (
    <Box
      sx={{
        minHeight: "100vh",
        py: 5,
        background:
          "radial-gradient(circle at top left, rgba(92,133,255,0.18), transparent 26%), linear-gradient(135deg, #e8eef2 0%, #dde9e3 100%)",
      }}
    >
      <Container maxWidth="xl">
        <Stack spacing={3}>
          <Card sx={{ bgcolor: "rgba(255,255,255,0.84)", backdropFilter: "blur(12px)" }}>
            <CardContent sx={{ p: { xs: 3, md: 4 } }}>
              <Stack direction="row" spacing={2} alignItems="center">
                <Box
                  sx={{
                    width: 56,
                    height: 56,
                    borderRadius: 3,
                    display: "grid",
                    placeItems: "center",
                    bgcolor: "primary.main",
                    color: "common.white",
                  }}
                >
                  <CategoryIcon />
                </Box>
                <Box>
                  <Typography
                    variant="overline"
                    color="primary.main"
                    sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                  >
                    {t("Admin panel")}
                  </Typography>
                  <Typography variant="h4">{t("References management")}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t("Logged in as {name} ({email})", {
                      name: `${user?.firstName ?? ""} ${user?.lastName ?? ""}`.trim(),
                      email: user?.email,
                    })}
                  </Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>

          <Card>
            <CardContent sx={{ p: 0 }}>
              <Tabs
                value={activeType}
                onChange={(_, nextType) => handleTypeChange(nextType)}
                variant="scrollable"
                scrollButtons="auto"
                sx={{ px: 2, borderBottom: 1, borderColor: "divider" }}
              >
                {referenceTypes.map((referenceType) => (
                  <Tab key={referenceType.type} value={referenceType.type} label={t(referenceType.label)} />
                ))}
              </Tabs>
            </CardContent>
          </Card>

          <Box
            sx={{
              display: "grid",
              gap: 3,
              gridTemplateColumns: { xs: "1fr", lg: "400px minmax(0, 1fr)" },
              alignItems: "start",
            }}
          >
            <Card>
              <CardContent sx={{ p: 3 }}>
                <Stack spacing={3} component="form" onSubmit={handleSubmit}>
                  <Box>
                    <Typography variant="h5">
                      {t("Create {label}", { label: t(config.label).toLowerCase() })}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                      {t("Adds a new record to the selected reference table.")}
                    </Typography>
                  </Box>

                  <TextField
                    label={t("Code")}
                    value={form.code}
                    onChange={(event) => setForm({ ...form, code: event.target.value })}
                    slotProps={{
                      htmlInput: { maxLength: 50 },
                    }}
                    required
                    fullWidth
                  />

                  {config.valueField ? (
                    <TextField
                      label={t(config.valueLabel)}
                      value={form.value}
                      onChange={(event) => {
                        let value = event.target.value.replace(",", ".");
                        if (value === ".") value = "0.";

                        if (/^\d*\.?\d{0,2}$/.test(value)) {
                          setForm({ ...form, value });
                        }
                      }}
                      inputMode="decimal"
                      required
                      fullWidth
                    />
                  ) : null}

                  {error ? <Alert severity="error">{error}</Alert> : null}

                  <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                    {isSubmitting ? t("Saving...") : t("Create reference")}
                  </Button>
                </Stack>
              </CardContent>
            </Card>

            <Card>
              <CardContent sx={{ p: 3 }}>
                <Stack spacing={2.5}>
                  <Stack
                    direction={{ xs: "column", sm: "row" }}
                    justifyContent="space-between"
                    alignItems={{ xs: "flex-start", sm: "center" }}
                    spacing={2}
                  >
                    <Box>
                      <Typography variant="h5">
                        {t("Existing {label}", { label: t(config.label).toLowerCase() })}
                      </Typography>
                    </Box>
                    <Button
                      variant="outlined"
                      startIcon={<RefreshIcon />}
                      onClick={() => loadReferences(pageIndex, activeType)}
                    >
                      {t("Refresh")}
                    </Button>
                  </Stack>

                  {feedback ? <Alert severity="success" onClose={() => setFeedback("")}>{feedback}</Alert> : null}

                  {isLoading ? (
                    <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                      <Stack spacing={2} alignItems="center">
                        <CircularProgress />
                        <Typography color="text.secondary">{t("Loading references...")}</Typography>
                      </Stack>
                    </Box>
                  ) : null}

                  {!isLoading && referencesPage?.content?.length ? (
                    <>
                      <TableContainer component={Paper} variant="outlined">
                        <Table>
                          <TableHead>
                            <TableRow>
                              <SortableTableCell sort={sort} field="code" onSort={handleSort}>{t("Code")}</SortableTableCell>
                              {config.valueField ? (
                                <SortableTableCell sort={sort} field={config.valueField} onSort={handleSort}>
                                  {t(config.valueLabel)}
                                </SortableTableCell>
                              ) : null}
                              <TableCell>{t("Action")}</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {sortedReferences.map((reference) => (
                              <TableRow key={reference.code} hover>
                                <TableCell>{formatReferenceCode(reference.code)}</TableCell>
                                {config.valueField ? <TableCell>{formatValue(reference)}</TableCell> : null}
                                <TableCell>
                                  <Button
                                    variant="contained"
                                    color="error"
                                    onClick={() => { deleteReference(reference.code); }}
                                  >
                                    {t("Delete")}
                                  </Button>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>

                      <Stack
                        direction={{ xs: "column", sm: "row" }}
                        justifyContent="space-between"
                        alignItems={{ xs: "flex-start", sm: "center" }}
                        spacing={2}
                      >
                        <Button
                          variant="outlined"
                          onClick={() => loadReferences(pageIndex - 1, activeType)}
                          disabled={referencesPage.first}
                        >
                          {t("Previous")}
                        </Button>
                        <Typography variant="body2" color="text.secondary">
                          {t("Page {current} of {total}", {
                            current: referencesPage.number + 1,
                            total: referencesPage.totalPages || 1,
                          })}
                        </Typography>
                        <Button
                          variant="outlined"
                          onClick={() => loadReferences(pageIndex + 1, activeType)}
                          disabled={referencesPage.last}
                        >
                          {t("Next")}
                        </Button>
                      </Stack>
                    </>
                  ) : null}

                  {!isLoading && !referencesPage?.content?.length ? (
                    <Alert severity="info">{t("No references found yet.")}</Alert>
                  ) : null}
                </Stack>
              </CardContent>
            </Card>
          </Box>
        </Stack>
      </Container>
    </Box>
  );
}
