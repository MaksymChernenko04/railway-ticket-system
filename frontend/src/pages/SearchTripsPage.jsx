import SearchIcon from "@mui/icons-material/Search";
import TrainIcon from "@mui/icons-material/Train";
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function toDateInputValue(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatDateTime(value) {
  if (!value) {
    return "";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatDuration(start, end) {
  const startDate = new Date(start);
  const endDate = new Date(end);
  const diffMinutes = Math.max(0, Math.round((endDate.getTime() - startDate.getTime()) / 60000));
  const hours = Math.floor(diffMinutes / 60);
  const minutes = diffMinutes % 60;

  if (hours === 0) {
    return `${minutes} min`;
  }

  return `${hours} h ${minutes} min`;
}

function buildSearchParams(form) {
  const params = new URLSearchParams();
  params.set("originStationId", form.originStationId);
  params.set("destinationStationId", form.destinationStationId);
  params.set("travelDate", form.travelDate);
  return params.toString();
}

export function SearchTripsPage() {
  const { authorizedGet, user } = useAuth();
  const navigate = useNavigate();
  const todayInputValue = useMemo(() => toDateInputValue(), []);
  const [stations, setStations] = useState([]);
  const [form, setForm] = useState({
    originStationId: "",
    destinationStationId: "",
    travelDate: todayInputValue,
  });
  const [originInput, setOriginInput] = useState("");
  const [destinationInput, setDestinationInput] = useState("");
  const [trips, setTrips] = useState([]);
  const [error, setError] = useState("");
  const [isLoadingStations, setIsLoadingStations] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const destinationStations = useMemo(
    () => stations.filter((station) => String(station.id) !== String(form.originStationId)),
    [stations, form.originStationId],
  );
  const selectedOriginStation = useMemo(
    () => stations.find((station) => String(station.id) === String(form.originStationId)) ?? null,
    [stations, form.originStationId],
  );
  const selectedDestinationStation = useMemo(
    () => stations.find((station) => String(station.id) === String(form.destinationStationId)) ?? null,
    [stations, form.destinationStationId],
  );

  useEffect(() => {
    let isActive = true;

    async function loadStations() {
      setIsLoadingStations(true);
      setError("");

      try {
        const data = await authorizedGet("/api/client/stations?page=0&size=1000&sort=name,asc");
        if (!isActive) {
          return;
        }

        const loadedStations = data.content ?? [];
        setStations(loadedStations);
      } catch (loadError) {
        if (isActive) {
          setError(loadError.message);
        }
      } finally {
        if (isActive) {
          setIsLoadingStations(false);
        }
      }
    }

    loadStations();

    return () => {
      isActive = false;
    };
  }, []);

  function updateForm(changes) {
    setForm((current) => {
      const next = { ...current, ...changes };
      if (next.originStationId && next.originStationId === next.destinationStationId) {
        next.destinationStationId = "";
      }
      return next;
    });
  }

  function formatStationOption(station) {
    return station ? `${station.name} (${station.countryCode})` : "";
  }

  async function handleSearch(event) {
    event.preventDefault();
    setError("");
    setTrips([]);
    setHasSearched(true);

    if (!form.originStationId || !form.destinationStationId || !form.travelDate) {
      setError("Origin, destination and travel date are required.");
      return;
    }

    if (form.originStationId === form.destinationStationId) {
      setError("Origin and destination stations must be different.");
      return;
    }

    if (form.travelDate < todayInputValue) {
      setError("Travel date cannot be in the past.");
      return;
    }

    setIsSearching(true);

    try {
      const data = await authorizedGet(`/api/client/trips/search?${buildSearchParams(form)}`);
      setTrips(data);
    } catch (searchError) {
      setError(searchError.message);
    } finally {
      setIsSearching(false);
    }
  }

  function openSeatSelection(trip) {
    const params = new URLSearchParams();
    params.set("startTripStopId", trip.departureTripStopId);
    params.set("endTripStopId", trip.arrivalTripStopId);
    navigate(`/trips/${trip.tripId}/seats?${params.toString()}`);
  }

  return (
    <Box
      sx={{
        minHeight: "calc(100vh - 64px)",
        py: 5,
        background:
          "radial-gradient(circle at top left, rgba(79,138,91,0.20), transparent 28%), linear-gradient(135deg, #e8eef2 0%, #dde9e3 100%)",
      }}
    >
      <Container maxWidth="xl">
        <Stack spacing={3}>
          <Card sx={{ bgcolor: "rgba(255,255,255,0.86)", backdropFilter: "blur(12px)" }}>
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
                      width: 58,
                      height: 58,
                      borderRadius: 3,
                      display: "grid",
                      placeItems: "center",
                      bgcolor: "primary.main",
                      color: "common.white",
                    }}
                  >
                    <SearchIcon />
                  </Box>
                  <Box>
                    <Typography
                      variant="overline"
                      color="primary.main"
                      sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                    >
                      Tickets
                    </Typography>
                    <Typography variant="h4">Search trips</Typography>
                    <Typography variant="body2" color="text.secondary">
                      Choose your route and travel date, then select seats.
                    </Typography>
                  </Box>
                </Stack>
                <Chip color="primary" variant="outlined" label={user?.email ?? "Client"} />
              </Stack>
            </CardContent>
          </Card>

          <Box
            sx={{
              display: "grid",
              gap: 3,
              gridTemplateColumns: { xs: "1fr", lg: "420px minmax(0, 1fr)" },
              alignItems: "start",
            }}
          >
            <Card>
              <CardContent sx={{ p: 3 }}>
                <Stack spacing={3} component="form" onSubmit={handleSearch}>
                  <Box>
                    <Typography variant="h5">Search details</Typography>
                  </Box>

                  <Autocomplete
                    options={stations}
                    value={selectedOriginStation}
                    inputValue={originInput}
                    open={
                      originInput.trim().length > 0 &&
                      originInput !== formatStationOption(selectedOriginStation)
                    }
                    disabled={isLoadingStations}
                    getOptionLabel={formatStationOption}
                    isOptionEqualToValue={(option, value) => String(option.id) === String(value.id)}
                    noOptionsText="No matching stations"
                    onInputChange={(_, value, reason) => {
                      setOriginInput(value);
                      if (reason === "input" && selectedOriginStation && value !== formatStationOption(selectedOriginStation)) {
                        updateForm({ originStationId: "" });
                      }
                    }}
                    onChange={(_, station) => {
                      if (station && String(station.id) === String(form.destinationStationId)) {
                        setDestinationInput("");
                      }
                      updateForm({ originStationId: station ? String(station.id) : "" });
                    }}
                    renderInput={(params) => (
                      <TextField {...params} label="Origin station" required fullWidth />
                    )}
                  />

                  <Autocomplete
                    options={destinationStations}
                    value={selectedDestinationStation}
                    inputValue={destinationInput}
                    open={
                      destinationInput.trim().length > 0 &&
                      destinationInput !== formatStationOption(selectedDestinationStation)
                    }
                    disabled={isLoadingStations}
                    getOptionLabel={formatStationOption}
                    isOptionEqualToValue={(option, value) => String(option.id) === String(value.id)}
                    noOptionsText="No matching stations"
                    onInputChange={(_, value, reason) => {
                      setDestinationInput(value);
                      if (
                        reason === "input" &&
                        selectedDestinationStation &&
                        value !== formatStationOption(selectedDestinationStation)
                      ) {
                        updateForm({ destinationStationId: "" });
                      }
                    }}
                    onChange={(_, station) => {
                      updateForm({ destinationStationId: station ? String(station.id) : "" });
                    }}
                    renderInput={(params) => (
                      <TextField {...params} label="Destination station" required fullWidth />
                    )}
                  />

                  <TextField
                    label="Travel date"
                    type="date"
                    value={form.travelDate}
                    onChange={(event) => updateForm({ travelDate: event.target.value })}
                    slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: todayInputValue } }}
                    required
                    fullWidth
                  />

                  {error ? <Alert severity="error">{error}</Alert> : null}

                  <Button
                    type="submit"
                    variant="contained"
                    size="large"
                    startIcon={<SearchIcon />}
                    disabled={isLoadingStations || isSearching}
                  >
                    {isSearching ? "Searching..." : "Search trips"}
                  </Button>
                </Stack>
              </CardContent>
            </Card>

            <Card>
              <CardContent sx={{ p: 3 }}>
                <Stack spacing={2.5}>
                  <Box>
                    <Typography variant="h5">Available trips</Typography>
                  </Box>

                  {isSearching ? (
                    <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                      <Stack spacing={2} alignItems="center">
                        <CircularProgress />
                        <Typography color="text.secondary">Searching trips...</Typography>
                      </Stack>
                    </Box>
                  ) : null}

                  {!isSearching && hasSearched && trips.length === 0 ? (
                    <Alert severity="info">No trips found for this route and date.</Alert>
                  ) : null}

                  {!isSearching && !hasSearched ? (
                    <Alert severity="info">Choose stations and date to start searching.</Alert>
                  ) : null}

                  {!isSearching && trips.length > 0 ? (
                    <Stack spacing={1.5}>
                      {trips.map((trip) => (
                        <Card key={`${trip.tripId}-${trip.departureTripStopId}-${trip.arrivalTripStopId}`} variant="outlined">
                          <CardContent sx={{ p: 2.5 }}>
                            <Stack spacing={2}>
                              <Stack
                                direction={{ xs: "column", md: "row" }}
                                justifyContent="space-between"
                                alignItems={{ xs: "flex-start", md: "center" }}
                                spacing={2}
                              >
                                <Stack direction="row" spacing={1.5} alignItems="center">
                                  <Box
                                    sx={{
                                      width: 46,
                                      height: 46,
                                      borderRadius: 2,
                                      display: "grid",
                                      placeItems: "center",
                                      bgcolor: "rgba(79,138,91,0.14)",
                                      color: "primary.dark",
                                    }}
                                  >
                                    <TrainIcon />
                                  </Box>
                                  <Box>
                                    <Typography variant="h6">
                                      {trip.trainNumber} - {trip.trainName || "Train"}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                      {trip.routeNumber} - {trip.routeName}
                                    </Typography>
                                  </Box>
                                </Stack>
                                <Chip
                                  color="primary"
                                  label={`${trip.basePrice} ${trip.currencyCode}`}
                                  sx={{ fontWeight: 700 }}
                                />
                              </Stack>

                              <Box
                                sx={{
                                  display: "grid",
                                  gridTemplateColumns: { xs: "1fr", md: "1fr auto 1fr" },
                                  gap: 2,
                                  alignItems: "center",
                                }}
                              >
                                <Box>
                                  <Typography variant="caption" color="text.secondary">
                                    Departure
                                  </Typography>
                                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                                    {trip.departureStationName}
                                  </Typography>
                                  <Typography variant="body2">{formatDateTime(trip.departureAtUtc)}</Typography>
                                </Box>
                                <Chip variant="outlined" label={formatDuration(trip.departureAtUtc, trip.arrivalAtUtc)} />
                                <Box sx={{ textAlign: { xs: "left", md: "right" } }}>
                                  <Typography variant="caption" color="text.secondary">
                                    Arrival
                                  </Typography>
                                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                                    {trip.arrivalStationName}
                                  </Typography>
                                  <Typography variant="body2">{formatDateTime(trip.arrivalAtUtc)}</Typography>
                                </Box>
                              </Box>

                              <Button variant="contained" onClick={() => openSeatSelection(trip)}>
                                Select seats
                              </Button>
                            </Stack>
                          </CardContent>
                        </Card>
                      ))}
                    </Stack>
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
