import SearchIcon from "@mui/icons-material/Search";
import TipsAndUpdatesIcon from "@mui/icons-material/TipsAndUpdates";
import TrainIcon from "@mui/icons-material/Train";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
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
import { useI18n } from "../i18n/I18nContext.jsx";

function toDateInputValue(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function addDaysToDateInputValue(value, days) {
  const [year, month, day] = String(value).split("-").map(Number);
  const date = new Date(year, month - 1, day);
  if (Number.isNaN(date.getTime())) {
    return toDateInputValue();
  }

  date.setDate(date.getDate() + days);
  return toDateInputValue(date);
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

function formatDuration(start, end, t) {
  const startDate = new Date(start);
  const endDate = new Date(end);
  const diffMinutes = Math.max(0, Math.round((endDate.getTime() - startDate.getTime()) / 60000));
  const hours = Math.floor(diffMinutes / 60);
  const minutes = diffMinutes % 60;

  if (hours === 0) {
    return t("{count} min", { count: minutes });
  }

  return t("{hours} h {minutes} min", { hours, minutes });
}

function buildSearchParams(form) {
  const params = new URLSearchParams();
  params.set("originStationId", form.originStationId);
  params.set("destinationStationId", form.destinationStationId);
  params.set("travelDate", form.travelDate);
  return params.toString();
}

function resolveRecommendationMessage(data, t) {
  const backendMessage = data?.message?.trim();
  if (backendMessage) {
    return backendMessage;
  }

  if (!data?.messageKey) {
    return "";
  }

  const translatedMessage = t(data.messageKey, data.messageArgs);
  return translatedMessage === data.messageKey ? "" : translatedMessage;
}

export function SearchTripsPage() {
  const { authorizedGet, user } = useAuth();
  const { t, language } = useI18n();
  const navigate = useNavigate();
  const todayInputValue = useMemo(() => toDateInputValue(), []);
  const shouldShowRecommendation = user?.role !== "ADMIN";
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
  const [isLoadingRecommendation, setIsLoadingRecommendation] = useState(true);
  const [recommendation, setRecommendation] = useState("");
  const [recommendationAction, setRecommendationAction] = useState(null);
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

    async function loadRecommendation() {
      if (!shouldShowRecommendation) {
        setIsLoadingRecommendation(false);
        setRecommendation("");
        setRecommendationAction(null);
        return;
      }

      setIsLoadingRecommendation(true);

      try {
        const data = await authorizedGet(
            `/api/client/assistant/search-recommendation?language=${encodeURIComponent(language)}`,
        );

        if (isActive) {
          const message = resolveRecommendationMessage(data, t);
          setRecommendation(
            message || t("Choose your route and travel date to find the best available trip."),
          );
          setRecommendationAction(
            data.actionType
              ? {
                  type: data.actionType,
                  params: data.actionParams ?? {},
                }
              : null,
          );
        }
      } catch {
        if (isActive) {
          setRecommendation(t("Choose your route and travel date to find the best available trip."));
          setRecommendationAction(null);
        }
      } finally {
        if (isActive) {
          setIsLoadingRecommendation(false);
        }
      }
    }

    loadRecommendation();

    return () => {
      isActive = false;
    };
  }, [language, authorizedGet, t, shouldShowRecommendation]);

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

  async function searchTrips(nextForm) {
    setError("");
    setTrips([]);
    setHasSearched(true);

    if (!nextForm.originStationId || !nextForm.destinationStationId || !nextForm.travelDate) {
      setError(t("Origin, destination and travel date are required."));
      return;
    }

    if (nextForm.originStationId === nextForm.destinationStationId) {
      setError(t("Origin and destination stations must be different."));
      return;
    }

    if (nextForm.travelDate < todayInputValue) {
      setError(t("Travel date cannot be in the past."));
      return;
    }

    setIsSearching(true);

    try {
      const data = await authorizedGet(`/api/client/trips/search?${buildSearchParams(nextForm)}`);
      setTrips(data);
    } catch (searchError) {
      setError(searchError.message);
    } finally {
      setIsSearching(false);
    }
  }

  async function handleSearch(event) {
    event.preventDefault();
    await searchTrips(form);
  }

  async function changeTravelDateBy(days) {
    const travelDate = addDaysToDateInputValue(form.travelDate, days);
    if (travelDate < todayInputValue) {
      return;
    }

    const nextForm = { ...form, travelDate };
    setForm(nextForm);
    if (hasSearched && nextForm.originStationId && nextForm.destinationStationId) {
      await searchTrips(nextForm);
    }
  }

  async function applyRecommendationAction() {
    if (recommendationAction?.type !== "trip_search") {
      return;
    }

    const params = recommendationAction.params;
    const nextForm = {
      originStationId: params.originStationId ?? "",
      destinationStationId: params.destinationStationId ?? "",
      travelDate: params.travelDate ?? todayInputValue,
    };
    setForm(nextForm);
    setOriginInput(params.originStationName ?? "");
    setDestinationInput(params.destinationStationName ?? "");
    await searchTrips(nextForm);
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
                      {t("Tickets")}
                    </Typography>
                    <Typography variant="h4">{t("Search trips")}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {t("Choose your route and travel date, then select seats.")}
                    </Typography>
                  </Box>
                </Stack>
                <Chip color="primary" variant="outlined" label={user?.email ?? t("Client")} />
              </Stack>
            </CardContent>
          </Card>

          {shouldShowRecommendation && (isLoadingRecommendation || recommendation) ? (
            <Card
              sx={{
                bgcolor: "rgba(24,84,63,0.94)",
                color: "common.white",
                border: "1px solid rgba(255,255,255,0.20)",
                boxShadow: "0 18px 44px rgba(24,84,63,0.22)",
              }}
            >
              <CardContent sx={{ p: { xs: 2.5, md: 3 } }}>
                <Stack direction={{ xs: "column", md: "row" }} spacing={2} alignItems={{ xs: "stretch", md: "flex-start" }}>
                  <Box
                    sx={{
                      width: 46,
                      height: 46,
                      borderRadius: 2,
                      display: "grid",
                      placeItems: "center",
                      bgcolor: "rgba(255,255,255,0.16)",
                      flex: "0 0 auto",
                    }}
                  >
                    {isLoadingRecommendation ? (
                      <CircularProgress size={22} sx={{ color: "common.white" }} />
                    ) : (
                      <TipsAndUpdatesIcon />
                    )}
                  </Box>
                  <Stack
                    direction={{ xs: "column", md: "row" }}
                    spacing={2}
                    alignItems={{ xs: "stretch", md: "center" }}
                    sx={{ flex: 1, minWidth: 0 }}
                  >
                    <Typography
                      variant="h6"
                      sx={{
                        flex: 1,
                        minWidth: 0,
                        fontWeight: 800,
                        lineHeight: 1.35,
                        whiteSpace: "normal",
                        overflowWrap: "break-word",
                      }}
                    >
                      {isLoadingRecommendation ? t("Preparing recommendation...") : recommendation}
                    </Typography>
                    {!isLoadingRecommendation && recommendationAction?.type === "trip_search" ? (
                      <Button
                        variant="contained"
                        color="secondary"
                        onClick={applyRecommendationAction}
                        disabled={isSearching}
                        sx={{ flex: "0 0 auto", whiteSpace: "nowrap" }}
                      >
                        {t("Search trips")}
                      </Button>
                    ) : null}
                  </Stack>
                </Stack>
              </CardContent>
            </Card>
          ) : null}

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
                    <Typography variant="h5">{t("Search details")}</Typography>
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
                    noOptionsText={t("No matching stations")}
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
                      <TextField {...params} label={t("Origin station")} required fullWidth />
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
                    noOptionsText={t("No matching stations")}
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
                      <TextField {...params} label={t("Destination station")} required fullWidth />
                    )}
                  />

                  <TextField
                    label={t("Travel date")}
                    type="date"
                    value={form.travelDate}
                    onChange={(event) => updateForm({ travelDate: event.target.value })}
                    slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: todayInputValue } }}
                    required
                    fullWidth
                  />

                  <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
                    <Button
                      type="button"
                      variant="outlined"
                      startIcon={<ChevronLeftIcon />}
                      onClick={() => changeTravelDateBy(-1)}
                      disabled={isSearching || form.travelDate <= todayInputValue}
                      sx={{ flex: 1 }}
                    >
                      {t("Previous day")}
                    </Button>
                    <Button
                      type="button"
                      variant="outlined"
                      endIcon={<ChevronRightIcon />}
                      onClick={() => changeTravelDateBy(1)}
                      disabled={isSearching}
                      sx={{ flex: 1 }}
                    >
                      {t("Next day")}
                    </Button>
                  </Stack>

                  {error ? <Alert severity="error">{error}</Alert> : null}

                  <Button
                    type="submit"
                    variant="contained"
                    size="large"
                    startIcon={<SearchIcon />}
                    disabled={isLoadingStations || isSearching}
                  >
                    {isSearching ? t("Searching...") : t("Search trips")}
                  </Button>
                </Stack>
              </CardContent>
            </Card>

            <Card>
              <CardContent sx={{ p: 3 }}>
                <Stack spacing={2.5}>
                  <Box>
                    <Typography variant="h5">{t("Available trips")}</Typography>
                  </Box>

                  {isSearching ? (
                    <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                      <Stack spacing={2} alignItems="center">
                        <CircularProgress />
                        <Typography color="text.secondary">{t("Searching trips...")}</Typography>
                      </Stack>
                    </Box>
                  ) : null}

                  {!isSearching && hasSearched && trips.length === 0 ? (
                    <Alert severity="info">{t("No trips found for this route and date.")}</Alert>
                  ) : null}

                  {!isSearching && !hasSearched ? (
                    <Alert severity="info">{t("Choose stations and date to start searching.")}</Alert>
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
                                      {trip.trainNumber} - {trip.trainName || t("Train")}
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
                                    {t("Departure")}
                                  </Typography>
                                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                                    {trip.departureStationName}
                                  </Typography>
                                  <Typography variant="body2">{formatDateTime(trip.departureAtUtc)}</Typography>
                                </Box>
                                <Chip variant="outlined" label={formatDuration(trip.departureAtUtc, trip.arrivalAtUtc, t)} />
                                <Box sx={{ textAlign: { xs: "left", md: "right" } }}>
                                  <Typography variant="caption" color="text.secondary">
                                    {t("Arrival")}
                                  </Typography>
                                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                                    {trip.arrivalStationName}
                                  </Typography>
                                  <Typography variant="body2">{formatDateTime(trip.arrivalAtUtc)}</Typography>
                                </Box>
                              </Box>

                              <Button variant="contained" onClick={() => openSeatSelection(trip)}>
                                {t("Select seats")}
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
