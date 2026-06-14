import RefreshIcon from "@mui/icons-material/Refresh";
import TrainIcon from "@mui/icons-material/Train";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";

const emptyForm = {
  name: "",
  countryCode: "",
  timezone: "Europe/Warsaw",
  address: "",
  accessible: true,
};

export function AdminStationsPage() {
  const { authorizedGet, authorizedPost, authorizedDelete, user } = useAuth();
  const [stationsPage, setStationsPage] = useState(null);
  const [countries, setCountries] = useState([]);
  const [timezones, setTimezones] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pageIndex, setPageIndex] = useState(0);

  async function loadStations(page = pageIndex) {
    setIsLoading(true);
    setError("");

    try {
      const data = await authorizedGet(`/api/admin/stations?page=${page}&size=8&sort=name,asc`);
      setStationsPage(data);
      setPageIndex(page);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setIsLoading(false);
    }
  }

  async function loadReferenceData() {
    try {
      const [loadedCountries, loadedTimezones] = await Promise.all([
        authorizedGet("/api/admin/stations/countries"),
        authorizedGet("/api/admin/stations/timezones"),
      ]);
      setCountries(loadedCountries);
      setTimezones(loadedTimezones);
    } catch (loadError) {
      setError(loadError.message);
    }
  }

  useEffect(() => {
    loadStations(0);
    loadReferenceData();
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    setFeedback("");
    setError("");
    setIsSubmitting(true);

    try {
      await authorizedPost("/api/admin/stations", {
        ...form,
      });
      setFeedback("Station created.");
      setForm(emptyForm);
      await loadStations(0);
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function deleteStation(id) {
    if (!globalThis.confirm("Are you sure you want to delete this station?")) {
      return;
    }

    setFeedback("");
    setError("");

    try {
      await authorizedDelete(`/api/admin/stations/${id}`);
      setFeedback("Station deleted.");
      
      const isLastItemOnPage = stationsPage?.content?.length === 1;
      const nextPageIndex = (isLastItemOnPage && pageIndex > 0) ? pageIndex - 1 : pageIndex;
      
      await loadStations(nextPageIndex);
    } catch (deleteError) {
      setError(deleteError.message);
    }
  }

  return (
    <Box
      sx={{
        minHeight: "100vh",
        py: 5,
        background:
            "radial-gradient(circle at top left, rgba(92,133,255,0.18), transparent 26%), linear-gradient(135deg, #f4f7fb 0%, #edf7f3 100%)",
      }}
    >
      <Container maxWidth="xl">
        <Stack spacing={3}>
          <Card sx={{ bgcolor: "rgba(255,255,255,0.84)", backdropFilter: "blur(12px)" }}>
            <CardContent sx={{ p: { xs: 3, md: 4 } }}>
              <Stack
                direction={{ xs: "column", md: "row" }}
                justifyContent="space-between"
                alignItems={{ xs: "flex-start", md: "center" }}
                spacing={2}
              >
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
                    <TrainIcon />
                  </Box>
                  <Box>
                    <Typography
                      variant="overline"
                      color="primary.main"
                      sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                    >
                      Admin panel
                    </Typography>
                    <Typography variant="h4">Stations management</Typography>
                    <Typography variant="body2" color="text.secondary">
                      Logged in as {user?.firstName} {user?.lastName} ({user?.email})
                    </Typography>
                  </Box>
                </Stack>
              </Stack>
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
                    <Typography variant="h5">Create station</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                      Add station details used in routes and schedules.
                    </Typography>
                  </Box>

                  <TextField
                    label="Name"
                    value={form.name}
                    onChange={(event) => setForm({ ...form, name: event.target.value })}
                    required
                    fullWidth
                  />

                  <FormControl fullWidth required>
                    <InputLabel id="station-country-label">Country</InputLabel>
                    <Select
                      labelId="station-country-label"
                      label="Country"
                      value={form.countryCode}
                      onChange={(event) => setForm({ ...form, countryCode: event.target.value })}
                    >
                      {countries.map((country) => (
                        <MenuItem key={country.code} value={country.code}>
                          {country.code}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <FormControl fullWidth required>
                    <InputLabel id="station-timezone-label">Timezone</InputLabel>
                    <Select
                      labelId="station-timezone-label"
                      label="Timezone"
                      value={form.timezone}
                      onChange={(event) => setForm({ ...form, timezone: event.target.value })}
                    >
                      {timezones.map((timezone) => (
                        <MenuItem key={timezone.code} value={timezone.code}>
                          {timezone.code}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <TextField
                    label="Address"
                    value={form.address}
                    onChange={(event) => setForm({ ...form, address: event.target.value })}
                    fullWidth
                  />

                  <FormControlLabel
                    control={
                      <Switch
                        checked={form.accessible}
                        onChange={(event) => setForm({ ...form, accessible: event.target.checked })}
                      />
                    }
                    label="Accessible station"
                  />

                  {error ? <Alert severity="error">{error}</Alert> : null}

                  <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                    {isSubmitting ? "Saving..." : "Create station"}
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
                      <Typography variant="h5">Existing stations</Typography>
                    </Box>
                    <Button
                      variant="outlined"
                      startIcon={<RefreshIcon />}
                      onClick={() => loadStations(pageIndex)}
                    >
                      Refresh
                    </Button>
                  </Stack>

                  {feedback ? <Alert severity="success" onClose={() => setFeedback("")}>{feedback}</Alert> : null}

                  {isLoading ? (
                    <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                      <Stack spacing={2} alignItems="center">
                        <CircularProgress />
                        <Typography color="text.secondary">Loading stations...</Typography>
                      </Stack>
                    </Box>
                  ) : null}

                  {!isLoading && stationsPage?.content?.length ? (
                    <>
                      <TableContainer component={Paper} variant="outlined">
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableCell>Name</TableCell>
                              <TableCell>Country</TableCell>
                              <TableCell>Timezone</TableCell>
                              <TableCell>Address</TableCell>
                              <TableCell>Accessible</TableCell>
                              <TableCell>Action</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {stationsPage.content.map((station) => (
                              <TableRow key={station.id} hover>
                                <TableCell>{station.name}</TableCell>
                                <TableCell>{station.countryCode}</TableCell>
                                <TableCell>{station.timezone}</TableCell>
                                <TableCell>{station.address}</TableCell>
                                <TableCell>
                                  <Chip
                                    size="small"
                                    color={station.accessible ? "success" : "default"}
                                    label={station.accessible ? "Yes" : "No"}
                                  />
                                </TableCell>
                                <TableCell>
                                  <Button
                                    variant="contained"
                                    color="error"
                                    onClick={() => {deleteStation(station.id)}}
                                  >
                                    Delete
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
                          onClick={() => loadStations(pageIndex - 1)}
                          disabled={stationsPage.first}
                        >
                          Previous
                        </Button>
                        <Typography variant="body2" color="text.secondary">
                          Page {stationsPage.number + 1} of {stationsPage.totalPages || 1}
                        </Typography>
                        <Button
                          variant="outlined"
                          onClick={() => loadStations(pageIndex + 1)}
                          disabled={stationsPage.last}
                        >
                          Next
                        </Button>
                      </Stack>
                    </>
                  ) : null}

                  {!isLoading && !stationsPage?.content?.length ? (
                    <Alert severity="info">No stations found yet.</Alert>
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
