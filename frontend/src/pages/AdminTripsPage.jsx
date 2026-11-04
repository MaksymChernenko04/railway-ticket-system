import AddIcon from "@mui/icons-material/Add";
import CloseIcon from "@mui/icons-material/Close";
import DeleteIcon from "@mui/icons-material/Delete";
import RefreshIcon from "@mui/icons-material/Refresh";
import TrainIcon from "@mui/icons-material/Train";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
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
  Tooltip,
  Typography,
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { nextSortState, SortableTableCell, sortRows } from "../components/SortableTableCell.jsx";
import { useI18n } from "../i18n/I18nContext.jsx";

const emptyForm = {
  trainId: "",
  routeId: "",
  departureAt: "",
};

const emptyCarPicker = {
  carId: "",
  ordinal: "",
};

function toDateTimeLocal(value) {
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

function toDateTimeParts(value) {
  if (!value) {
    return { date: "", time: "" };
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return { date: value, time: "" };
  }

  return {
    date: date.toLocaleDateString(undefined, {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }),
    time: date.toLocaleTimeString(undefined, {
      hour: "2-digit",
      minute: "2-digit",
    }),
  };
}

function formatSeatSummary(seats) {
  if (!seats?.length) {
    return "No seats";
  }

  return seats
    .slice()
    .sort((left, right) => Number(left.number) - Number(right.number))
    .map((seat) => seat.number)
    .join(", ");
}

function buildStops(routeStops) {
  return routeStops.map((routeStop, index) => {
    const baseOffset = index * 20;

    return {
      routeStopId: String(routeStop.id),
      stationName: routeStop.stationName,
      stationCountryCode: routeStop.stationCountryCode,
      ordinal: routeStop.ordinal,
      selected: true,
      platform: "1",
      track: "1",
      arrivalOffsetMinutes: String(baseOffset),
      departureOffsetMinutes: String(baseOffset + (index === 0 ? 0 : 5)),
    };
  });
}

function sortByColumn(left, right) {
  return Number(left.column) - Number(right.column);
}

function groupSeatsByRow(seats) {
  const rows = new Map();
  seats.forEach((seat) => {
    const rowKey = Number(seat.row) || 1;
    rows.set(rowKey, [...(rows.get(rowKey) ?? []), seat]);
  });

  return [...rows.entries()]
    .sort(([left], [right]) => left - right)
    .map(([row, rowSeats]) => [row, rowSeats.sort(sortByColumn)]);
}

function getSeatByColumn(rowSeats, column) {
  return rowSeats.find((seat) => Number(seat.column) === column);
}

function renderCorridor(sx) {
  return (
    <Box
      sx={{
        borderRadius: 1.5,
        bgcolor: "rgba(112,126,136,0.24)",
        border: "1px solid rgba(70,84,92,0.34)",
        ...sx,
      }}
    />
  );
}

function selectableSeatSx(seat, selected, orientation = "seat", readOnly = false, level = "seat") {
  const isLong = orientation === "forward";
  const isBerth = orientation === "berth" || orientation === "forward";
  const shadowByDirection = {
    FORWARD: "inset 6px 0 0 rgba(79,138,91,0.36)",
    BACKWARD: "inset -6px 0 0 rgba(79,138,91,0.36)",
    SIDEWAYS: "inset 0 6px 0 rgba(79,138,91,0.36)",
  };

  return {
    width: isBerth ? (isLong ? 106 : 46) : 44,
    height: isBerth ? (isLong ? 34 : 62) : 44,
    borderRadius: 2,
    border: selected ? "2px solid #2f6339" : "1px solid rgba(47,99,57,0.44)",
    bgcolor: selected ? "rgba(79,138,91,0.30)" : "rgba(255,255,255,0.92)",
    color: "text.primary",
    display: "grid",
    placeItems: "center",
    cursor: readOnly ? "default" : "pointer",
    font: "inherit",
    p: 0,
    boxShadow: isBerth
      ? level === "upper"
        ? "inset 0 5px 0 rgba(38,62,133,0.55)"
        : "inset 0 -5px 0 rgba(79,138,91,0.32)"
      : shadowByDirection[seat.direction] ?? shadowByDirection.FORWARD,
    transition: "background-color 160ms ease, border-color 160ms ease, transform 160ms ease",
    "&:hover": readOnly
      ? {}
      : {
          bgcolor: selected ? "rgba(79,138,91,0.36)" : "rgba(79,138,91,0.16)",
          transform: "translateY(-1px)",
        },
  };
}

function renderSelectableSeat(
  seat,
  selectedSeatIds,
  onToggleSeat,
  t,
  codeLabel,
  orientation = "seat",
  readOnly = false,
  level = "seat",
) {
  if (!seat) {
    const isLong = orientation === "forward";
    const isBerth = orientation === "berth" || orientation === "forward";

    return (
      <Box
        sx={{
          width: isBerth ? (isLong ? 106 : 46) : 44,
          height: isBerth ? (isLong ? 34 : 62) : 44,
          borderRadius: 2,
          border: "1px dashed rgba(88,96,92,0.34)",
          bgcolor: "rgba(112,126,136,0.10)",
        }}
      />
    );
  }

  const selected = selectedSeatIds.includes(String(seat.id));

  return (
    <Tooltip
      title={`${t("Seat")} ${seat.number}, ${codeLabel("seatType", seat.typeCode)}${
        level === "seat" ? "" : `, ${t(level)}`
      }, ${codeLabel("direction", seat.direction)}`}
    >
      <Box
        component={readOnly ? "div" : "button"}
        {...(readOnly ? {} : { type: "button", onClick: () => onToggleSeat(seat.id) })}
        sx={selectableSeatSx(seat, selected, orientation, readOnly, level)}
      >
        <Typography variant="caption" sx={{ fontWeight: 800, lineHeight: 1 }}>
          {seat.number}
        </Typography>
      </Box>
    </Tooltip>
  );
}

function renderCoupeBlock(rowNumber, rowSeats, lux, selectedSeatIds, onToggleSeat, t, codeLabel, readOnly = false) {
  const leftLower = getSeatByColumn(rowSeats, 1);
  const leftUpper = lux ? null : getSeatByColumn(rowSeats, 2);
  const rightLower = getSeatByColumn(rowSeats, lux ? 2 : 3);
  const rightUpper = lux ? null : getSeatByColumn(rowSeats, 4);

  return (
    <Stack spacing={0.75} alignItems="center">
      <Typography variant="caption" color="text.secondary">
        {t(lux ? "Compartment" : "Coupe")} {rowNumber}
      </Typography>
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "46px 18px 46px",
          gap: 0.75,
          alignItems: "center",
          p: 0.75,
          borderRadius: 2,
          border: "1px solid rgba(47,99,57,0.36)",
          bgcolor: "rgba(246,249,244,0.94)",
        }}
      >
        <Stack spacing={0.5}>
          {lux ? null : renderSelectableSeat(leftUpper, selectedSeatIds, onToggleSeat, t, codeLabel, "berth", readOnly, "upper")}
          {renderSelectableSeat(leftLower, selectedSeatIds, onToggleSeat, t, codeLabel, "berth", readOnly, "lower")}
        </Stack>
        {renderCorridor({ width: 18, height: lux ? 62 : 126 })}
        <Stack spacing={0.5}>
          {lux ? null : renderSelectableSeat(rightUpper, selectedSeatIds, onToggleSeat, t, codeLabel, "berth", readOnly, "upper")}
          {renderSelectableSeat(rightLower, selectedSeatIds, onToggleSeat, t, codeLabel, "berth", readOnly, "lower")}
        </Stack>
      </Box>
    </Stack>
  );
}

function renderSleeperScheme(tripCar, onToggleSeat, t, codeLabel, readOnly = false) {
  const rows = groupSeatsByRow(tripCar.seats);
  const carType = tripCar.carTypeCode?.toUpperCase();
  const isLux = carType === "LUX_SLEEPER";
  const isOpen = carType === "OPEN_SLEEPER";

  return (
    <Box sx={{ minWidth: Math.max(760, rows.length * 138), pb: 1 }}>
      <Stack direction="row" spacing={1.5} alignItems="flex-start">
        {rows.map(([row, rowSeats]) => (
          <Stack key={row} spacing={0.75} alignItems="center">
            {renderCoupeBlock(row, rowSeats, isLux, tripCar.seatIds, onToggleSeat, t, codeLabel, readOnly)}
            {isOpen ? (
              <>
                {renderCorridor({ width: 126, height: 22 })}
                <Stack spacing={0.5}>
                  {renderSelectableSeat(
                    getSeatByColumn(rowSeats, 6),
                    tripCar.seatIds,
                    onToggleSeat,
                    t,
                    codeLabel,
                    "forward",
                    readOnly,
                    "upper",
                  )}
                  {renderSelectableSeat(
                    getSeatByColumn(rowSeats, 5),
                    tripCar.seatIds,
                    onToggleSeat,
                    t,
                    codeLabel,
                    "forward",
                    readOnly,
                    "lower",
                  )}
                </Stack>
              </>
            ) : (
              renderCorridor({ width: 126, height: 24 })
            )}
          </Stack>
        ))}
      </Stack>
    </Box>
  );
}

function renderFirstClassRow(rowSeats, selectedSeatIds, onToggleSeat, t, codeLabel, readOnly = false) {
  const orderedSeats = [1, 2, 3].map((column) => getSeatByColumn(rowSeats, column));
  const presentSeats = orderedSeats.filter(Boolean);
  const directionSplitIndex = presentSeats.findIndex(
    (seat, index) => index > 0 && seat.direction !== presentSeats[index - 1].direction,
  );
  const splitIndex = directionSplitIndex > 0 ? directionSplitIndex : 1;
  const leftColumns = orderedSeats.slice(0, splitIndex);
  const rightColumns = orderedSeats.slice(splitIndex);

  return (
    <Stack spacing={0.5} alignItems="center">
      {leftColumns.map((seat, index) => (
        <Box key={seat?.id ?? `left-empty-${index}`}>
          {renderSelectableSeat(seat, selectedSeatIds, onToggleSeat, t, codeLabel, "seat", readOnly)}
        </Box>
      ))}
      {renderCorridor({ width: 44, height: 22 })}
      {rightColumns.map((seat, index) => (
        <Box key={seat?.id ?? `right-empty-${index}`}>
          {renderSelectableSeat(seat, selectedSeatIds, onToggleSeat, t, codeLabel, "seat", readOnly)}
        </Box>
      ))}
    </Stack>
  );
}

function renderSittingScheme(tripCar, onToggleSeat, t, codeLabel, readOnly = false) {
  const rows = groupSeatsByRow(tripCar.seats);
  const isFirstClass = tripCar.carTypeCode?.toUpperCase() === "SITTING_CLASS_1";

  return (
    <Box sx={{ minWidth: Math.max(760, rows.length * (isFirstClass ? 86 : 92)), pb: 1 }}>
      <Stack direction="row" spacing={1.5} alignItems="flex-start">
        {rows.map(([row, rowSeats]) => (
          <Stack key={row} spacing={0.75} alignItems="center">
            <Typography variant="caption" color="text.secondary">
              {t("Row")} {row}
            </Typography>
            {isFirstClass ? (
              renderFirstClassRow(rowSeats, tripCar.seatIds, onToggleSeat, t, codeLabel, readOnly)
            ) : (
              <Stack spacing={0.5} alignItems="center">
                {renderSelectableSeat(getSeatByColumn(rowSeats, 1), tripCar.seatIds, onToggleSeat, t, codeLabel, "seat", readOnly)}
                {renderSelectableSeat(getSeatByColumn(rowSeats, 2), tripCar.seatIds, onToggleSeat, t, codeLabel, "seat", readOnly)}
                {renderCorridor({ width: 44, height: 22 })}
                {renderSelectableSeat(getSeatByColumn(rowSeats, 3), tripCar.seatIds, onToggleSeat, t, codeLabel, "seat", readOnly)}
                {renderSelectableSeat(getSeatByColumn(rowSeats, 4), tripCar.seatIds, onToggleSeat, t, codeLabel, "seat", readOnly)}
              </Stack>
            )}
          </Stack>
        ))}
      </Stack>
    </Box>
  );
}

function renderGenericScheme(tripCar, onToggleSeat, t, codeLabel, readOnly = false) {
  const rows = groupSeatsByRow(tripCar.seats);

  return (
    <Box sx={{ minWidth: Math.max(620, rows.length * 96), pb: 1 }}>
      <Stack direction="row" spacing={1.2} alignItems="flex-start">
        {rows.map(([row, rowSeats]) => (
          <Stack key={row} spacing={0.75} alignItems="center">
            <Typography variant="caption" color="text.secondary">
              {t("Row")} {row}
            </Typography>
            {rowSeats.map((seat) => (
              <Box key={seat.id}>{renderSelectableSeat(seat, tripCar.seatIds, onToggleSeat, t, codeLabel, "seat", readOnly)}</Box>
            ))}
          </Stack>
        ))}
      </Stack>
    </Box>
  );
}

function renderTripSeatScheme(tripCar, onToggleSeat, t, codeLabel, readOnly = false) {
  const carType = tripCar.carTypeCode?.toUpperCase();
  const isSleeper = ["COUPE_SLEEPER", "LUX_SLEEPER", "OPEN_SLEEPER"].includes(carType);
  const isSitting = ["SITTING_CLASS_1", "SITTING_CLASS_2"].includes(carType);

  if (tripCar.seats.length === 0) {
    return <Alert severity="info">{t("This car has no seats yet.")}</Alert>;
  }

  return (
    <Box
      sx={{
        overflowX: "auto",
        border: "1px solid rgba(47,99,57,0.30)",
        borderRadius: 2,
        bgcolor: "rgba(240,246,241,0.88)",
        p: 2,
      }}
    >
      {isSleeper ? renderSleeperScheme(tripCar, onToggleSeat, t, codeLabel, readOnly) : null}
      {isSitting ? renderSittingScheme(tripCar, onToggleSeat, t, codeLabel, readOnly) : null}
      {isSleeper || isSitting ? null : renderGenericScheme(tripCar, onToggleSeat, t, codeLabel, readOnly)}
    </Box>
  );
}

export function AdminTripsPage() {
  const { authorizedGet, authorizedPost, authorizedDelete, user } = useAuth();
  const { t, codeLabel } = useI18n();
  const [tripsPage, setTripsPage] = useState(null);
  const [trains, setTrains] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [cars, setCars] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [stops, setStops] = useState([]);
  const [carPicker, setCarPicker] = useState(emptyCarPicker);
  const [tripCars, setTripCars] = useState([]);
  const [selectedTrip, setSelectedTrip] = useState(null);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const [detailsError, setDetailsError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isReferenceLoading, setIsReferenceLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDetailsLoading, setIsDetailsLoading] = useState(false);
  const [pageIndex, setPageIndex] = useState(0);
  const [sort, setSort] = useState({ field: "departureAtUtc", direction: "desc" });
  const [detailsStopsSort, setDetailsStopsSort] = useState({ field: "ordinal", direction: "asc" });

  const selectedRoute = useMemo(
    () => routes.find((route) => String(route.id) === String(form.routeId)),
    [routes, form.routeId],
  );

  const selectedCarIds = useMemo(() => new Set(tripCars.map((tripCar) => String(tripCar.carId))), [tripCars]);
  const selectableCars = cars.filter((car) => !selectedCarIds.has(String(car.id)));
  const selectedStops = stops.filter((stop) => stop.selected);
  const sortedTrips = useMemo(
    () => sortRows(tripsPage?.content ?? [], sort, {
      stopCount: (trip) => Number(trip.stopCount),
      carCount: (trip) => Number(trip.carCount),
      seatCount: (trip) => Number(trip.seatCount),
    }),
    [tripsPage, sort],
  );
  const sortedDetailsStops = useMemo(
    () => sortRows(selectedTrip?.stops ?? [], detailsStopsSort, {
      ordinal: (stop) => Number(stop.ordinal),
      platform: (stop) => Number(stop.platform),
      track: (stop) => Number(stop.track),
      arrivalOffsetMinutes: (stop) => Number(stop.arrivalOffsetMinutes),
      departureOffsetMinutes: (stop) => Number(stop.departureOffsetMinutes),
    }),
    [selectedTrip, detailsStopsSort],
  );

  async function loadTrips(page = pageIndex) {
    setIsLoading(true);
    setError("");

    try {
      const data = await authorizedGet(`/api/admin/trips?page=${page}&size=8&sort=departureAtUtc,desc`);
      setTripsPage(data);
      setPageIndex(page);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setIsLoading(false);
    }
  }

  async function loadReferenceData() {
    setIsReferenceLoading(true);

    try {
      const [loadedTrains, loadedRoutes, loadedCars] = await Promise.all([
        authorizedGet("/api/admin/trains?page=0&size=1000&sort=number,asc"),
        authorizedGet("/api/admin/routes?page=0&size=1000&sort=number,asc"),
        authorizedGet("/api/admin/cars?page=0&size=1000&sort=number,asc"),
      ]);

      setTrains(loadedTrains.content ?? []);
      setRoutes(loadedRoutes.content ?? []);
      setCars(loadedCars.content ?? []);

      const firstRouteId = loadedRoutes.content?.[0]?.id ? String(loadedRoutes.content[0].id) : "";
      const firstTrainId = loadedTrains.content?.[0]?.id ? String(loadedTrains.content[0].id) : "";
      setForm((current) => ({ ...current, routeId: firstRouteId, trainId: firstTrainId }));

      if (firstRouteId) {
        await loadRouteStops(firstRouteId);
      }
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setIsReferenceLoading(false);
    }
  }

  async function loadRouteStops(routeId) {
    if (!routeId) {
      setStops([]);
      return;
    }

    const data = await authorizedGet(`/api/admin/route-stops?routeId=${routeId}&page=0&size=1000&sort=ordinal,asc`);
    setStops(buildStops(data.content ?? []));
  }

  useEffect(() => {
    loadTrips(0);
    loadReferenceData();
  }, []);

  async function handleRouteChange(routeId) {
    setForm((current) => ({ ...current, routeId }));
    setStops([]);
    setError("");

    try {
      await loadRouteStops(routeId);
    } catch (loadError) {
      setError(loadError.message);
    }
  }

  function updateStop(routeStopId, changes) {
    setStops((current) =>
      current.map((stop) => (stop.routeStopId === routeStopId ? { ...stop, ...changes } : stop)),
    );
  }

  async function addTripCar() {
    if (!carPicker.carId || !carPicker.ordinal) {
      setError(t("Select car and ordinal before adding it to trip."));
      return;
    }

    setError("");

    try {
      const car = cars.find((item) => String(item.id) === String(carPicker.carId));
      const seats = await authorizedGet(`/api/admin/cars/${carPicker.carId}/seats`);

      setTripCars((current) => [
        ...current,
        {
          carId: String(carPicker.carId),
          ordinal: String(carPicker.ordinal),
          carNumber: car?.number ?? carPicker.carId,
          carTypeCode: car?.typeCode ?? "",
          seats,
          seatIds: seats.map((seat) => String(seat.id)),
        },
      ]);
      setCarPicker(emptyCarPicker);
    } catch (loadError) {
      setError(loadError.message);
    }
  }

  function updateTripCar(carId, changes) {
    setTripCars((current) =>
      current.map((tripCar) => (tripCar.carId === carId ? { ...tripCar, ...changes } : tripCar)),
    );
  }

  function removeTripCar(carId) {
    setTripCars((current) => current.filter((tripCar) => tripCar.carId !== carId));
  }

  function toggleTripSeat(carId, seatId) {
    const normalizedSeatId = String(seatId);
    setTripCars((current) =>
      current.map((tripCar) => {
        if (tripCar.carId !== carId) {
          return tripCar;
        }

        const selected = tripCar.seatIds.includes(normalizedSeatId);
        const seatIds = selected
          ? tripCar.seatIds.filter((currentSeatId) => currentSeatId !== normalizedSeatId)
          : [...tripCar.seatIds, normalizedSeatId];

        return { ...tripCar, seatIds };
      }),
    );
  }

  function selectAllTripSeats(carId) {
    updateTripCar(carId, {
      seatIds: tripCars.find((tripCar) => tripCar.carId === carId)?.seats.map((seat) => String(seat.id)) ?? [],
    });
  }

  function clearTripSeats(carId) {
    updateTripCar(carId, { seatIds: [] });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setFeedback("");
    setError("");

    if (!form.departureAt) {
      setError(t("Departure date and time are required."));
      return;
    }

    if (selectedStops.length === 0) {
      setError(t("Select at least one station from the route."));
      return;
    }

    if (tripCars.length === 0) {
      setError(t("Add at least one car to the trip."));
      return;
    }

    if (tripCars.some((tripCar) => tripCar.seatIds.length === 0)) {
      setError(t("Select at least one seat for every trip car."));
      return;
    }

    setIsSubmitting(true);

    try {
      await authorizedPost("/api/admin/trips", {
        trainId: Number(form.trainId),
        routeId: Number(form.routeId),
        departureAtUtc: new Date(form.departureAt).toISOString(),
        stops: selectedStops.map((stop) => ({
          routeStopId: Number(stop.routeStopId),
          platform: Number(stop.platform),
          track: Number(stop.track),
          arrivalOffsetMinutes: Number(stop.arrivalOffsetMinutes),
          departureOffsetMinutes: Number(stop.departureOffsetMinutes),
        })),
        cars: tripCars.map((tripCar) => ({
          carId: Number(tripCar.carId),
          ordinal: Number(tripCar.ordinal),
          seatIds: tripCar.seatIds.map(Number),
        })),
      });

      setFeedback(t("Trip created."));
      setForm((current) => ({ trainId: current.trainId, routeId: current.routeId, departureAt: "" }));
      setTripCars([]);
      await loadRouteStops(form.routeId);
      await loadTrips(0);
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function deleteTrip(id) {
    if (!globalThis.confirm(t("Are you sure you want to delete this trip?"))) {
      return;
    }

    setFeedback("");
    setError("");

    try {
      await authorizedDelete(`/api/admin/trips/${id}`);
      setFeedback(t("Trip deleted."));

      const isLastItemOnPage = tripsPage?.content?.length === 1;
      const nextPageIndex = isLastItemOnPage && pageIndex > 0 ? pageIndex - 1 : pageIndex;
      await loadTrips(nextPageIndex);
    } catch (deleteError) {
      setError(deleteError.message);
    }
  }

  async function openTripDetails(id) {
    setSelectedTrip(null);
    setDetailsError("");
    setIsDetailsLoading(true);

    try {
      const data = await authorizedGet(`/api/admin/trips/${id}`);
      setSelectedTrip(data);
    } catch (loadError) {
      setDetailsError(loadError.message);
    } finally {
      setIsDetailsLoading(false);
    }
  }

  function closeTripDetails() {
    setSelectedTrip(null);
    setDetailsError("");
    setIsDetailsLoading(false);
  }

  function handleSort(field) {
    setSort((current) => nextSortState(current, field));
  }

  function handleDetailsStopsSort(field) {
    setDetailsStopsSort((current) => nextSortState(current, field));
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
                  <TrainIcon />
                </Box>
                <Box>
                  <Typography variant="overline" color="primary.main" sx={{ letterSpacing: "0.16em", fontWeight: 700 }}>
                    {t("Admin panel")}
                  </Typography>
                  <Typography variant="h4">{t("Trips management")}</Typography>
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

          <Box
            sx={{
              display: "grid",
              gap: 3,
              gridTemplateColumns: { xs: "1fr", xl: "520px minmax(0, 1fr)" },
              alignItems: "start",
            }}
          >
            <Card>
              <CardContent sx={{ p: 3 }}>
                <Stack spacing={3} component="form" onSubmit={handleSubmit}>
                  <Box>
                    <Typography variant="h5">{t("Create trip")}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                      {t("Creates trip, trip stops, trip cars, and trip seats in one request.")}
                    </Typography>
                  </Box>

                  <FormControl fullWidth required disabled={isReferenceLoading}>
                    <InputLabel id="trip-train-label">{t("Train")}</InputLabel>
                    <Select
                      labelId="trip-train-label"
                      label={t("Train")}
                      value={form.trainId}
                      onChange={(event) => setForm({ ...form, trainId: event.target.value })}
                    >
                      {trains.map((train) => (
                        <MenuItem key={train.id} value={String(train.id)}>
                          {train.number} - {train.name} ({codeLabel("trainType", train.typeCode)})
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <FormControl fullWidth required disabled={isReferenceLoading}>
                    <InputLabel id="trip-route-label">{t("Route")}</InputLabel>
                    <Select
                      labelId="trip-route-label"
                      label={t("Route")}
                      value={form.routeId}
                      onChange={(event) => handleRouteChange(event.target.value)}
                    >
                      {routes.map((route) => (
                        <MenuItem key={route.id} value={String(route.id)}>
                          {route.number} - {route.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <TextField
                    label={t("Departure date and time")}
                    type="datetime-local"
                    value={form.departureAt}
                    onChange={(event) => setForm({ ...form, departureAt: event.target.value })}
                    slotProps={{ inputLabel: { shrink: true } }}
                    required
                    fullWidth
                  />

                  <Stack spacing={1.5}>
                    <Box>
                      <Typography variant="h6">{t("Stations and offsets")}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {selectedRoute ? `${selectedRoute.number} - ${selectedRoute.name}` : t("Select route")}
                      </Typography>
                    </Box>

                    {!stops.length ? <Alert severity="info">{t("Selected route has no stations.")}</Alert> : null}

                    {stops.map((stop) => (
                      <Paper key={stop.routeStopId} variant="outlined" sx={{ p: 1.5 }}>
                        <Stack spacing={1.25}>
                          <Stack direction="row" alignItems="center" spacing={1}>
                            <Checkbox
                              checked={stop.selected}
                              onChange={(event) => updateStop(stop.routeStopId, { selected: event.target.checked })}
                            />
                            <Box sx={{ minWidth: 0 }}>
                              <Typography variant="subtitle2">
                                {stop.ordinal}. {stop.stationName} ({stop.stationCountryCode})
                              </Typography>
                            </Box>
                          </Stack>
                          <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
                            <TextField
                              label={t("Platform")}
                              type="number"
                              size="small"
                              value={stop.platform}
                              onChange={(event) => updateStop(stop.routeStopId, { platform: event.target.value })}
                              slotProps={{ htmlInput: { min: 1 } }}
                              required={stop.selected}
                              disabled={!stop.selected}
                            />
                            <TextField
                              label={t("Track")}
                              type="number"
                              size="small"
                              value={stop.track}
                              onChange={(event) => updateStop(stop.routeStopId, { track: event.target.value })}
                              slotProps={{ htmlInput: { min: 1 } }}
                              required={stop.selected}
                              disabled={!stop.selected}
                            />
                            <TextField
                              label={t("Arrival offset")}
                              type="number"
                              size="small"
                              value={stop.arrivalOffsetMinutes}
                              onChange={(event) =>
                                updateStop(stop.routeStopId, { arrivalOffsetMinutes: event.target.value })
                              }
                              slotProps={{ htmlInput: { min: 0 } }}
                              required={stop.selected}
                              disabled={!stop.selected}
                            />
                            <TextField
                              label={t("Departure offset")}
                              type="number"
                              size="small"
                              value={stop.departureOffsetMinutes}
                              onChange={(event) =>
                                updateStop(stop.routeStopId, { departureOffsetMinutes: event.target.value })
                              }
                              slotProps={{ htmlInput: { min: 0 } }}
                              required={stop.selected}
                              disabled={!stop.selected}
                            />
                          </Stack>
                        </Stack>
                      </Paper>
                    ))}
                  </Stack>

                  <Stack spacing={1.5}>
                    <Typography variant="h6">{t("Cars and seats")}</Typography>
                    <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25} alignItems={{ sm: "flex-end" }}>
                      <FormControl fullWidth>
                        <InputLabel id="trip-car-label">{t("Car")}</InputLabel>
                        <Select
                          labelId="trip-car-label"
                          label={t("Car")}
                          value={carPicker.carId}
                          onChange={(event) => setCarPicker({ ...carPicker, carId: event.target.value })}
                        >
                          {selectableCars.map((car) => (
                            <MenuItem key={car.id} value={String(car.id)}>
                              {car.number} ({codeLabel("carType", car.typeCode)})
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                      <TextField
                        label={t("Ordinal")}
                        type="number"
                        value={carPicker.ordinal}
                        onChange={(event) => setCarPicker({ ...carPicker, ordinal: event.target.value })}
                        slotProps={{ htmlInput: { min: 1 } }}
                        sx={{ minWidth: 120 }}
                      />
                      <Button
                        variant="outlined"
                        startIcon={<AddIcon />}
                        onClick={addTripCar}
                        sx={{ minWidth: { sm: 132 }, minHeight: 56 }}
                      >
                        {t("Add")}
                      </Button>
                    </Stack>

                    {tripCars.map((tripCar) => (
                      <Paper key={tripCar.carId} variant="outlined" sx={{ p: 1.5 }}>
                        <Stack spacing={1.5}>
                          <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1}>
                            <Box>
                              <Typography variant="subtitle2">
                                {t("Car")} {tripCar.ordinal}: {tripCar.carNumber} ({codeLabel("carType", tripCar.carTypeCode)})
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                {t("{count} of {total} seats selected", {
                                  count: tripCar.seatIds.length,
                                  total: tripCar.seats.length,
                                })}
                              </Typography>
                            </Box>
                            <Stack direction="row" spacing={1} alignItems="center">
                              <Button size="small" variant="outlined" onClick={() => selectAllTripSeats(tripCar.carId)}>
                                {t("Select all")}
                              </Button>
                              <Button size="small" variant="outlined" onClick={() => clearTripSeats(tripCar.carId)}>
                                {t("Clear")}
                              </Button>
                              <IconButton color="error" onClick={() => removeTripCar(tripCar.carId)}>
                                <DeleteIcon />
                              </IconButton>
                            </Stack>
                          </Stack>

                          {renderTripSeatScheme(
                            tripCar,
                            (seatId) => toggleTripSeat(tripCar.carId, seatId),
                            t,
                            codeLabel,
                          )}
                        </Stack>
                      </Paper>
                    ))}
                  </Stack>

                  {error ? <Alert severity="error">{error}</Alert> : null}

                  <Button type="submit" variant="contained" size="large" disabled={isSubmitting || isReferenceLoading}>
                    {isSubmitting ? t("Saving...") : t("Create trip")}
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
                    <Typography variant="h5">{t("Existing trips")}</Typography>
                    <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => loadTrips(pageIndex)}>
                      {t("Refresh")}
                    </Button>
                  </Stack>

                  {feedback ? <Alert severity="success" onClose={() => setFeedback("")}>{feedback}</Alert> : null}

                  {isLoading ? (
                    <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                      <Stack spacing={2} alignItems="center">
                        <CircularProgress />
                        <Typography color="text.secondary">{t("Loading trips...")}</Typography>
                      </Stack>
                    </Box>
                  ) : null}

                  {!isLoading && tripsPage?.content?.length ? (
                    <>
                      <TableContainer component={Paper} variant="outlined">
                        <Table>
                          <TableHead>
                            <TableRow>
                              <SortableTableCell sort={sort} field="departureAtUtc" onSort={handleSort}>{t("Departure")}</SortableTableCell>
                              <SortableTableCell sort={sort} field="trainNumber" onSort={handleSort}>{t("Train")}</SortableTableCell>
                              <SortableTableCell sort={sort} field="routeNumber" onSort={handleSort}>{t("Route")}</SortableTableCell>
                              <SortableTableCell sort={sort} field="stopCount" onSort={handleSort}>{t("Stops")}</SortableTableCell>
                              <SortableTableCell sort={sort} field="carCount" onSort={handleSort}>{t("Cars")}</SortableTableCell>
                              <SortableTableCell sort={sort} field="seatCount" onSort={handleSort}>{t("Seats")}</SortableTableCell>
                              <TableCell>{t("Action")}</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {sortedTrips.map((trip) => {
                              const departure = toDateTimeParts(trip.departureAtUtc);

                              return (
                                <TableRow key={trip.id} hover>
                                  <TableCell sx={{ whiteSpace: "nowrap" }}>
                                    <Stack spacing={0.25}>
                                      <Typography variant="body2">{departure.date}</Typography>
                                      <Typography variant="caption" color="text.secondary">
                                        {departure.time}
                                      </Typography>
                                    </Stack>
                                  </TableCell>
                                  <TableCell>
                                    {trip.trainNumber} - {trip.trainName}
                                  </TableCell>
                                  <TableCell>
                                    {trip.routeNumber} - {trip.routeName}
                                  </TableCell>
                                  <TableCell>{trip.stopCount}</TableCell>
                                  <TableCell>{trip.carCount}</TableCell>
                                  <TableCell>{trip.seatCount}</TableCell>
                                  <TableCell>
                                    <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
                                      <Button variant="outlined" onClick={() => openTripDetails(trip.id)}>
                                        {t("Details")}
                                      </Button>
                                      <Button variant="contained" color="error" onClick={() => deleteTrip(trip.id)}>
                                        {t("Delete")}
                                      </Button>
                                    </Stack>
                                  </TableCell>
                                </TableRow>
                              );
                            })}
                          </TableBody>
                        </Table>
                      </TableContainer>

                      <Stack
                        direction={{ xs: "column", sm: "row" }}
                        justifyContent="space-between"
                        alignItems={{ xs: "flex-start", sm: "center" }}
                        spacing={2}
                      >
                        <Button variant="outlined" onClick={() => loadTrips(pageIndex - 1)} disabled={tripsPage.first}>
                          {t("Previous")}
                        </Button>
                        <Typography variant="body2" color="text.secondary">
                          {t("Page {current} of {total}", {
                            current: tripsPage.number + 1,
                            total: tripsPage.totalPages || 1,
                          })}
                        </Typography>
                        <Button variant="outlined" onClick={() => loadTrips(pageIndex + 1)} disabled={tripsPage.last}>
                          {t("Next")}
                        </Button>
                      </Stack>
                    </>
                  ) : null}

                  {!isLoading && !tripsPage?.content?.length ? (
                    <Alert severity="info">{t("No trips found yet.")}</Alert>
                  ) : null}
                </Stack>
              </CardContent>
            </Card>
          </Box>
        </Stack>
      </Container>

      <Dialog open={Boolean(selectedTrip) || isDetailsLoading || Boolean(detailsError)} onClose={closeTripDetails} fullWidth maxWidth="lg">
        <DialogTitle sx={{ pr: 7 }}>
          <Stack spacing={0.5}>
            <Typography variant="h5">{t("Trip details")}</Typography>
            <Typography variant="body2" color="text.secondary">
              {selectedTrip
                ? t("Trip {train} on {route}, {date}", {
                    train: selectedTrip.trainNumber,
                    route: selectedTrip.routeNumber,
                    date: toDateTimeLocal(selectedTrip.departureAtUtc),
                  })
                : ""}
            </Typography>
          </Stack>
          <IconButton aria-label={t("Close trip details")} onClick={closeTripDetails} sx={{ position: "absolute", right: 16, top: 16 }}>
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent dividers>
          {isDetailsLoading ? (
            <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
              <CircularProgress />
            </Box>
          ) : null}

          {detailsError ? <Alert severity="error">{detailsError}</Alert> : null}

          {selectedTrip ? (
            <Stack spacing={3}>
              <Box>
                <Typography variant="h6" sx={{ mb: 1.5 }}>{t("Stops")}</Typography>
                <TableContainer component={Paper} variant="outlined">
                  <Table>
                    <TableHead>
                      <TableRow>
                        <SortableTableCell sort={detailsStopsSort} field="ordinal" onSort={handleDetailsStopsSort}>{t("Station")}</SortableTableCell>
                        <SortableTableCell sort={detailsStopsSort} field="platform" onSort={handleDetailsStopsSort}>{t("Platform")}</SortableTableCell>
                        <SortableTableCell sort={detailsStopsSort} field="track" onSort={handleDetailsStopsSort}>{t("Track")}</SortableTableCell>
                        <SortableTableCell sort={detailsStopsSort} field="arrivalAtUtc" onSort={handleDetailsStopsSort}>{t("Arrival")}</SortableTableCell>
                        <SortableTableCell sort={detailsStopsSort} field="departureAtUtc" onSort={handleDetailsStopsSort}>{t("Departure")}</SortableTableCell>
                        <SortableTableCell sort={detailsStopsSort} field="arrivalOffsetMinutes" onSort={handleDetailsStopsSort}>{t("Offsets")}</SortableTableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {sortedDetailsStops.map((stop) => (
                        <TableRow key={stop.id}>
                          <TableCell>
                            {stop.ordinal}. {stop.stationName} ({stop.stationCountryCode})
                          </TableCell>
                          <TableCell>{stop.platform}</TableCell>
                          <TableCell>{stop.track}</TableCell>
                          <TableCell>{toDateTimeLocal(stop.arrivalAtUtc)}</TableCell>
                          <TableCell>{toDateTimeLocal(stop.departureAtUtc)}</TableCell>
                          <TableCell>
                            +{stop.arrivalOffsetMinutes} / +{stop.departureOffsetMinutes} min
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>

              <Box>
                <Typography variant="h6" sx={{ mb: 1.5 }}>{t("Cars")}</Typography>
                <Stack spacing={1.5}>
                  {selectedTrip.cars.map((tripCar) => (
                    <Paper key={tripCar.id} variant="outlined" sx={{ p: 2 }}>
                      <Stack spacing={1.25}>
                        <Stack direction={{ xs: "column", sm: "row" }} spacing={1} alignItems={{ sm: "center" }}>
                          <Typography variant="subtitle1">
                            {tripCar.ordinal}. {t("Car")} {tripCar.carNumber} ({codeLabel("carType", tripCar.carTypeCode)})
                          </Typography>
                          {tripCar.accessible ? <Chip size="small" color="success" label={t("Accessible")} /> : null}
                          <Chip size="small" variant="outlined" label={t("{count} seats", { count: tripCar.seats.length })} />
                        </Stack>
                        {renderTripSeatScheme(
                          {
                            ...tripCar,
                            seatIds: tripCar.seats.map((seat) => String(seat.id)),
                          },
                          () => {},
                          t,
                          codeLabel,
                          true,
                        )}
                      </Stack>
                    </Paper>
                  ))}
                </Stack>
              </Box>
            </Stack>
          ) : null}
        </DialogContent>
      </Dialog>
    </Box>
  );
}
