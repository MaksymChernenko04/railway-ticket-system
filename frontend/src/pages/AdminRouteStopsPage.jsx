import RefreshIcon from "@mui/icons-material/Refresh";
import RouteIcon from "@mui/icons-material/AltRoute";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Container,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
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

const emptyForm = {
  routeId: "",
  stationId: "",
  ordinal: "",
};

export function AdminRouteStopsPage() {
  const { authorizedGet, authorizedPost, authorizedDelete, user } = useAuth();
  const [routeStopsPage, setRouteStopsPage] = useState(null);
  const [routes, setRoutes] = useState([]);
  const [stations, setStations] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [selectedRouteId, setSelectedRouteId] = useState("");
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pageIndex, setPageIndex] = useState(0);
  const [sort, setSort] = useState({ field: "ordinal", direction: "asc" });

  const selectedRoute = useMemo(
    () => routes.find((route) => String(route.id) === String(selectedRouteId)),
    [routes, selectedRouteId],
  );
  const sortedRouteStops = useMemo(
    () => sortRows(routeStopsPage?.content ?? [], sort, { ordinal: (routeStop) => Number(routeStop.ordinal) }),
    [routeStopsPage, sort],
  );

  async function loadRouteStops(page = pageIndex, routeId = selectedRouteId) {
    setIsLoading(true);
    setError("");

    const routeFilter = routeId ? `&routeId=${routeId}` : "";

    try {
      const data = await authorizedGet(`/api/admin/route-stops?page=${page}&size=8&sort=ordinal,asc${routeFilter}`);
      setRouteStopsPage(data);
      setPageIndex(page);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setIsLoading(false);
    }
  }

  async function loadReferenceData() {
    try {
      const [loadedRoutes, loadedStations] = await Promise.all([
        authorizedGet("/api/admin/routes?page=0&size=1000&sort=number,asc"),
        authorizedGet("/api/admin/stations?page=0&size=1000&sort=name,asc"),
      ]);

      setRoutes(loadedRoutes.content ?? []);
      setStations(loadedStations.content ?? []);

      const firstRouteId = loadedRoutes.content?.[0]?.id ? String(loadedRoutes.content[0].id) : "";
      if (firstRouteId) {
        setSelectedRouteId(firstRouteId);
        setForm((current) => ({ ...current, routeId: firstRouteId }));
        await loadRouteStops(0, firstRouteId);
      } else {
        await loadRouteStops(0, "");
      }
    } catch (loadError) {
      setError(loadError.message);
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadReferenceData();
  }, []);

  async function handleRouteFilterChange(routeId) {
    setSelectedRouteId(routeId);
    setForm((current) => ({ ...current, routeId }));
    await loadRouteStops(0, routeId);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setFeedback("");
    setError("");
    setIsSubmitting(true);

    try {
      await authorizedPost("/api/admin/route-stops", {
        routeId: Number(form.routeId),
        stationId: Number(form.stationId),
        ordinal: Number(form.ordinal),
      });
      setFeedback("Route stop added.");
      setForm({ routeId: form.routeId, stationId: "", ordinal: "" });
      setSelectedRouteId(form.routeId);
      await loadRouteStops(0, form.routeId);
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function deleteRouteStop(id) {
    if (!globalThis.confirm("Are you sure you want to delete this route stop?")) {
      return;
    }

    setFeedback("");
    setError("");

    try {
      await authorizedDelete(`/api/admin/route-stops/${id}`);
      setFeedback("Route stop deleted.");

      const isLastItemOnPage = routeStopsPage?.content?.length === 1;
      const nextPageIndex = (isLastItemOnPage && pageIndex > 0) ? pageIndex - 1 : pageIndex;

      await loadRouteStops(nextPageIndex);
    } catch (deleteError) {
      setError(deleteError.message);
    }
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
                  <RouteIcon />
                </Box>
                <Box>
                  <Typography
                    variant="overline"
                    color="primary.main"
                    sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                  >
                    Admin panel
                  </Typography>
                  <Typography variant="h4">Route stops management</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Logged in as {user?.firstName} {user?.lastName} ({user?.email})
                  </Typography>
                </Box>
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
                    <Typography variant="h5">Add stop to route</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                      Builds the route scheme by assigning stations to ordered positions.
                    </Typography>
                  </Box>

                  <FormControl fullWidth required>
                    <InputLabel id="route-stop-route-label">Route</InputLabel>
                    <Select
                      labelId="route-stop-route-label"
                      label="Route"
                      value={form.routeId}
                      onChange={(event) => setForm({ ...form, routeId: event.target.value })}
                    >
                      {routes.map((route) => (
                        <MenuItem key={route.id} value={String(route.id)}>
                          {route.number} - {route.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <FormControl fullWidth required>
                    <InputLabel id="route-stop-station-label">Station</InputLabel>
                    <Select
                      labelId="route-stop-station-label"
                      label="Station"
                      value={form.stationId}
                      onChange={(event) => setForm({ ...form, stationId: event.target.value })}
                    >
                      {stations.map((station) => (
                        <MenuItem key={station.id} value={String(station.id)}>
                          {station.name} ({station.countryCode})
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <TextField
                    label="Ordinal"
                    type="number"
                    value={form.ordinal}
                    onChange={(event) => setForm({ ...form, ordinal: event.target.value })}
                    slotProps={{
                      htmlInput: { min: 1 },
                    }}
                    required
                    fullWidth
                  />

                  {error ? <Alert severity="error">{error}</Alert> : null}

                  <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                    {isSubmitting ? "Saving..." : "Add route stop"}
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
                      <Typography variant="h5">Route scheme</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {selectedRoute ? `${selectedRoute.number} - ${selectedRoute.name}` : "All routes"}
                      </Typography>
                    </Box>
                    <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} sx={{ width: { xs: "100%", sm: "auto" } }}>
                      <FormControl size="small" sx={{ minWidth: 220 }}>
                        <InputLabel id="route-stop-filter-label">Route</InputLabel>
                        <Select
                          labelId="route-stop-filter-label"
                          label="Route"
                          value={selectedRouteId}
                          onChange={(event) => handleRouteFilterChange(event.target.value)}
                        >
                          <MenuItem value="">All routes</MenuItem>
                          {routes.map((route) => (
                            <MenuItem key={route.id} value={String(route.id)}>
                              {route.number} - {route.name}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                      <Button
                        variant="outlined"
                        startIcon={<RefreshIcon />}
                        onClick={() => loadRouteStops(pageIndex)}
                      >
                        Refresh
                      </Button>
                    </Stack>
                  </Stack>

                  {feedback ? <Alert severity="success" onClose={() => setFeedback("")}>{feedback}</Alert> : null}

                  {isLoading ? (
                    <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                      <Stack spacing={2} alignItems="center">
                        <CircularProgress />
                        <Typography color="text.secondary">Loading route stops...</Typography>
                      </Stack>
                    </Box>
                  ) : null}

                  {!isLoading && routeStopsPage?.content?.length ? (
                    <>
                      <TableContainer component={Paper} variant="outlined">
                        <Table>
                          <TableHead>
                            <TableRow>
                              <SortableTableCell sort={sort} field="ordinal" onSort={handleSort}>Ordinal</SortableTableCell>
                              <SortableTableCell sort={sort} field="routeNumber" onSort={handleSort}>Route</SortableTableCell>
                              <SortableTableCell sort={sort} field="stationName" onSort={handleSort}>Station</SortableTableCell>
                              <SortableTableCell sort={sort} field="stationCountryCode" onSort={handleSort}>Country</SortableTableCell>
                              <TableCell>Action</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {sortedRouteStops.map((routeStop) => (
                              <TableRow key={routeStop.id} hover>
                                <TableCell>{routeStop.ordinal}</TableCell>
                                <TableCell>{routeStop.routeNumber}</TableCell>
                                <TableCell>{routeStop.stationName}</TableCell>
                                <TableCell>{routeStop.stationCountryCode}</TableCell>
                                <TableCell>
                                  <Button
                                    variant="contained"
                                    color="error"
                                    onClick={() => { deleteRouteStop(routeStop.id); }}
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
                          onClick={() => loadRouteStops(pageIndex - 1)}
                          disabled={routeStopsPage.first}
                        >
                          Previous
                        </Button>
                        <Typography variant="body2" color="text.secondary">
                          Page {routeStopsPage.number + 1} of {routeStopsPage.totalPages || 1}
                        </Typography>
                        <Button
                          variant="outlined"
                          onClick={() => loadRouteStops(pageIndex + 1)}
                          disabled={routeStopsPage.last}
                        >
                          Next
                        </Button>
                      </Stack>
                    </>
                  ) : null}

                  {!isLoading && !routeStopsPage?.content?.length ? (
                    <Alert severity="info">No route stops found yet.</Alert>
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
