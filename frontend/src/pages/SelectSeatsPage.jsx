import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import EventSeatIcon from "@mui/icons-material/EventSeat";
import PaymentIcon from "@mui/icons-material/Payment";
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
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { PassengerManager } from "../components/PassengerManager";

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

function compareSeatNumbers(left, right) {
  const leftNumber = Number(left.number);
  const rightNumber = Number(right.number);

  if (Number.isNaN(leftNumber) || Number.isNaN(rightNumber)) {
    return String(left.number).localeCompare(String(right.number));
  }

  return leftNumber - rightNumber;
}

function formatMoney(amount, currencyCode) {
  const value = Number(amount ?? 0);
  return `${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currencyCode ?? ""}`.trim();
}

const directionLabel = {
  FORWARD: "Forward",
  BACKWARD: "Backward",
  SIDEWAYS: "Sideways",
};

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
        bgcolor: "rgba(90,108,95,0.08)",
        border: "1px solid rgba(90,108,95,0.12)",
        ...sx,
      }}
    />
  );
}

function seatVisualSx(seat, selected, orientation = "seat", level = "seat") {
  const isLong = orientation === "forward";
  const isBerth = orientation === "berth" || orientation === "forward";
  const unavailable = seat && !seat.available;
  const directionShadow = {
    FORWARD: "inset 6px 0 0 rgba(79,138,91,0.36)",
    BACKWARD: "inset -6px 0 0 rgba(79,138,91,0.36)",
    SIDEWAYS: "inset 0 6px 0 rgba(79,138,91,0.36)",
  };
  const levelShadow = level === "upper"
    ? "inset 0 5px 0 rgba(38,62,133,0.55)"
    : "inset 0 -5px 0 rgba(79,138,91,0.32)";

  if (unavailable) {
    return {
      width: isBerth ? (isLong ? 106 : 46) : 44,
      height: isBerth ? (isLong ? 34 : 62) : 44,
      borderRadius: 2,
      border: "1px solid rgba(90,108,95,0.18)",
      bgcolor: "rgba(90,108,95,0.13)",
      color: "text.secondary",
      display: "grid",
      placeItems: "center",
      cursor: "not-allowed",
      opacity: 0.62,
      p: 0,
      boxShadow: isBerth ? "inset 0 0 0 999px rgba(90,108,95,0.08)" : "none",
    };
  }

  return {
    width: isBerth ? (isLong ? 106 : 46) : 44,
    height: isBerth ? (isLong ? 34 : 62) : 44,
    borderRadius: 2,
    border: selected ? "2px solid #3f744a" : "1px solid rgba(79,138,91,0.34)",
    bgcolor: selected ? "rgba(79,138,91,0.24)" : "rgba(255,255,255,0.78)",
    color: "text.primary",
    display: "grid",
    placeItems: "center",
    cursor: "pointer",
    font: "inherit",
    p: 0,
    boxShadow: `${isBerth ? levelShadow : directionShadow[seat?.direction] ?? directionShadow.FORWARD}${
      selected ? ", 0 10px 22px rgba(79,138,91,0.18)" : ""
    }`,
    transition: "background-color 160ms ease, border-color 160ms ease, transform 160ms ease",
    "&:hover": {
      bgcolor: selected ? "rgba(79,138,91,0.28)" : "rgba(79,138,91,0.12)",
      transform: "translateY(-1px)",
    },
  };
}

function renderSeatPlaceholder(orientation = "seat") {
  const isLong = orientation === "forward";
  const isBerth = orientation === "berth" || orientation === "forward";

  return (
    <Box
      sx={{
        width: isBerth ? (isLong ? 106 : 46) : 44,
        height: isBerth ? (isLong ? 34 : 62) : 44,
        borderRadius: 2,
        border: "1px dashed rgba(90,108,95,0.18)",
        bgcolor: "rgba(90,108,95,0.04)",
      }}
    />
  );
}

function renderAvailableSeat(seat, selectedTripSeatIds, onToggleSeat, orientation = "seat", level = "seat") {
  if (!seat) {
    return renderSeatPlaceholder(orientation);
  }

  const selected = selectedTripSeatIds.includes(String(seat.tripSeatId));
  const status = seat.available ? "available" : "unavailable";

  return (
    <Tooltip
      title={`Seat ${seat.number}, ${seat.seatTypeCode}${level === "seat" ? "" : `, ${level}`}, ${
        directionLabel[seat.direction] ?? seat.direction
      }, ${status}`}
    >
      <Box
        component="button"
        type="button"
        disabled={!seat.available}
        onClick={() => onToggleSeat(seat)}
        sx={seatVisualSx(seat, selected, orientation, level)}
      >
        <Typography variant="caption" sx={{ fontWeight: 800, lineHeight: 1 }}>
          {seat.number}
        </Typography>
      </Box>
    </Tooltip>
  );
}

function renderCoupeBlock(rowNumber, rowSeats, lux, selectedTripSeatIds, onToggleSeat) {
  const leftLower = getSeatByColumn(rowSeats, 1);
  const leftUpper = lux ? null : getSeatByColumn(rowSeats, 2);
  const rightLower = getSeatByColumn(rowSeats, lux ? 2 : 3);
  const rightUpper = lux ? null : getSeatByColumn(rowSeats, 4);

  return (
    <Stack spacing={0.75} alignItems="center">
      <Typography variant="caption" color="text.secondary">
        {lux ? "Compartment" : "Coupe"} {rowNumber}
      </Typography>
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "46px 18px 46px",
          gap: 0.75,
          alignItems: "center",
          p: 0.75,
          borderRadius: 2,
          border: "1px solid rgba(79,138,91,0.18)",
          bgcolor: "rgba(255,255,255,0.68)",
        }}
      >
        <Stack spacing={0.5}>
          {lux ? null : renderAvailableSeat(leftUpper, selectedTripSeatIds, onToggleSeat, "berth", "upper")}
          {renderAvailableSeat(leftLower, selectedTripSeatIds, onToggleSeat, "berth", "lower")}
        </Stack>
        {renderCorridor({ width: 18, height: lux ? 62 : 126 })}
        <Stack spacing={0.5}>
          {lux ? null : renderAvailableSeat(rightUpper, selectedTripSeatIds, onToggleSeat, "berth", "upper")}
          {renderAvailableSeat(rightLower, selectedTripSeatIds, onToggleSeat, "berth", "lower")}
        </Stack>
      </Box>
    </Stack>
  );
}

function renderSleeperScheme(car, selectedTripSeatIds, onToggleSeat) {
  const rows = groupSeatsByRow(car.seats);
  const carType = car.carTypeCode?.toUpperCase();
  const isLux = carType === "LUX_SLEEPER";
  const isOpen = carType === "OPEN_SLEEPER";

  return (
    <Box sx={{ minWidth: Math.max(760, rows.length * 138), pb: 1 }}>
      <Stack direction="row" spacing={1.5} alignItems="flex-start">
        {rows.map(([row, rowSeats]) => (
          <Stack key={row} spacing={0.75} alignItems="center">
            {renderCoupeBlock(row, rowSeats, isLux, selectedTripSeatIds, onToggleSeat)}
            {isOpen ? (
              <>
                {renderCorridor({ width: 126, height: 22 })}
                <Stack spacing={0.5}>
                  {renderAvailableSeat(getSeatByColumn(rowSeats, 6), selectedTripSeatIds, onToggleSeat, "forward", "upper")}
                  {renderAvailableSeat(getSeatByColumn(rowSeats, 5), selectedTripSeatIds, onToggleSeat, "forward", "lower")}
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

function renderFirstClassRow(rowSeats, selectedTripSeatIds, onToggleSeat) {
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
        <Box key={seat?.tripSeatId ?? `left-empty-${index}`}>
          {renderAvailableSeat(seat, selectedTripSeatIds, onToggleSeat)}
        </Box>
      ))}
      {renderCorridor({ width: 44, height: 22 })}
      {rightColumns.map((seat, index) => (
        <Box key={seat?.tripSeatId ?? `right-empty-${index}`}>
          {renderAvailableSeat(seat, selectedTripSeatIds, onToggleSeat)}
        </Box>
      ))}
    </Stack>
  );
}

function renderSittingScheme(car, selectedTripSeatIds, onToggleSeat) {
  const rows = groupSeatsByRow(car.seats);
  const isFirstClass = car.carTypeCode?.toUpperCase() === "SITTING_CLASS_1";

  return (
    <Box sx={{ minWidth: Math.max(760, rows.length * (isFirstClass ? 86 : 92)), pb: 1 }}>
      <Stack direction="row" spacing={1.5} alignItems="flex-start">
        {rows.map(([row, rowSeats]) => (
          <Stack key={row} spacing={0.75} alignItems="center">
            <Typography variant="caption" color="text.secondary">
              Row {row}
            </Typography>
            {isFirstClass ? (
              renderFirstClassRow(rowSeats, selectedTripSeatIds, onToggleSeat)
            ) : (
              <Stack spacing={0.5} alignItems="center">
                {renderAvailableSeat(getSeatByColumn(rowSeats, 1), selectedTripSeatIds, onToggleSeat)}
                {renderAvailableSeat(getSeatByColumn(rowSeats, 2), selectedTripSeatIds, onToggleSeat)}
                {renderCorridor({ width: 44, height: 22 })}
                {renderAvailableSeat(getSeatByColumn(rowSeats, 3), selectedTripSeatIds, onToggleSeat)}
                {renderAvailableSeat(getSeatByColumn(rowSeats, 4), selectedTripSeatIds, onToggleSeat)}
              </Stack>
            )}
          </Stack>
        ))}
      </Stack>
    </Box>
  );
}

function renderGenericScheme(car, selectedTripSeatIds, onToggleSeat) {
  const rows = groupSeatsByRow(car.seats);

  return (
    <Box sx={{ minWidth: Math.max(620, rows.length * 96), pb: 1 }}>
      <Stack direction="row" spacing={1.2} alignItems="flex-start">
        {rows.map(([row, rowSeats]) => (
          <Stack key={row} spacing={0.75} alignItems="center">
            <Typography variant="caption" color="text.secondary">
              Row {row}
            </Typography>
            {rowSeats.map((seat) => (
              <Box key={seat.tripSeatId}>{renderAvailableSeat(seat, selectedTripSeatIds, onToggleSeat)}</Box>
            ))}
          </Stack>
        ))}
      </Stack>
    </Box>
  );
}

function renderSeatScheme(car, selectedTripSeatIds, onToggleSeat) {
  const carType = car.carTypeCode?.toUpperCase();
  const isSleeper = ["COUPE_SLEEPER", "LUX_SLEEPER", "OPEN_SLEEPER"].includes(carType);
  const isSitting = ["SITTING_CLASS_1", "SITTING_CLASS_2"].includes(carType);

  if (car.seats.length === 0) {
    return <Alert severity="info">This car has no seats yet.</Alert>;
  }

  return (
    <Box
      sx={{
        overflowX: "auto",
        border: "1px solid rgba(79,138,91,0.18)",
        borderRadius: 2,
        bgcolor: "rgba(255,255,255,0.72)",
        p: 2,
      }}
    >
      {isSleeper ? renderSleeperScheme(car, selectedTripSeatIds, onToggleSeat) : null}
      {isSitting ? renderSittingScheme(car, selectedTripSeatIds, onToggleSeat) : null}
      {isSleeper || isSitting ? null : renderGenericScheme(car, selectedTripSeatIds, onToggleSeat)}
    </Box>
  );
}

export function SelectSeatsPage() {
  const { tripId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { authorizedGet, authorizedPost, authorizedDelete } = useAuth();
  const startTripStopId = searchParams.get("startTripStopId");
  const endTripStopId = searchParams.get("endTripStopId");
  const [trip, setTrip] = useState(null);
  const [availability, setAvailability] = useState(null);
  const [passengers, setPassengers] = useState([]);
  const [benefits, setBenefits] = useState([]);
  const [selectedTripSeatIds, setSelectedTripSeatIds] = useState([]);
  const [seatPassengerIds, setSeatPassengerIds] = useState({});
  const [order, setOrder] = useState(null);
  const [checkoutError, setCheckoutError] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("CARD");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isCreatingOrder, setIsCreatingOrder] = useState(false);
  const [isStartingPayment, setIsStartingPayment] = useState(false);
  const createOrderInFlightRef = useRef(false);

  const segment = useMemo(() => {
    const stops = trip?.stops ?? [];
    return {
      start: stops.find((stop) => String(stop.id) === String(startTripStopId)),
      end: stops.find((stop) => String(stop.id) === String(endTripStopId)),
    };
  }, [trip, startTripStopId, endTripStopId]);

  const selectedSeats = useMemo(() => {
    const selectedSet = new Set(selectedTripSeatIds.map(String));
    return (availability?.cars ?? [])
      .flatMap((car) =>
        car.seats
          .filter((seat) => selectedSet.has(String(seat.tripSeatId)))
          .map((seat) => ({
            ...seat,
            carNumber: car.carNumber,
            carOrdinal: car.ordinal,
          })),
      )
      .sort(compareSeatNumbers);
  }, [availability, selectedTripSeatIds]);

  useEffect(() => {
    const selectedSet = new Set(selectedTripSeatIds.map(String));
    setSeatPassengerIds((current) =>
      Object.fromEntries(Object.entries(current).filter(([tripSeatId]) => selectedSet.has(String(tripSeatId)))),
    );
    setOrder(null);
  }, [selectedTripSeatIds]);

  useEffect(() => {
    let isActive = true;

    async function loadData() {
      setError("");
      setIsLoading(true);
      setTrip(null);
      setAvailability(null);
      setPassengers([]);
      setBenefits([]);
      setSelectedTripSeatIds([]);
      setSeatPassengerIds({});
      setOrder(null);
      setCheckoutError("");

      if (!tripId || !startTripStopId || !endTripStopId) {
        setError("Please return to search and choose a trip again.");
        setIsLoading(false);
        return;
      }

      try {
        const [loadedTrip, loadedAvailability, loadedPassengers, loadedBenefits] = await Promise.all([
          authorizedGet(`/api/client/trips/${tripId}`),
          authorizedGet(
            `/api/client/trips/${tripId}/available-seats?startTripStopId=${startTripStopId}&endTripStopId=${endTripStopId}`,
          ),
          authorizedGet("/api/client/passengers"),
          authorizedGet("/api/client/passengers/benefits"),
        ]);

        if (!isActive) {
          return;
        }

        setTrip(loadedTrip);
        setAvailability(loadedAvailability);
        setPassengers(loadedPassengers);
        setBenefits(loadedBenefits);
      } catch (loadError) {
        if (isActive) {
          setError(loadError.message);
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    loadData();

    return () => {
      isActive = false;
    };
  }, [tripId, startTripStopId, endTripStopId]);

  function toggleSeat(seat) {
    if (order) {
      return;
    }

    if (!seat.available) {
      return;
    }

    const normalizedId = String(seat.tripSeatId);
    setSelectedTripSeatIds((current) =>
      current.includes(normalizedId)
        ? current.filter((id) => id !== normalizedId)
        : [...current, normalizedId],
    );
  }

  async function createPassenger(payload) {
    setCheckoutError("");
    try {
      const createdPassenger = await authorizedPost("/api/client/passengers", payload);
      setPassengers((current) => [...current, createdPassenger]);
    } catch (createError) {
      setCheckoutError(createError.message);
      throw createError;
    }
  }

  async function deletePassenger(passengerId) {
    setCheckoutError("");
    try {
      await authorizedDelete(`/api/client/passengers/${passengerId}`);
      setPassengers((current) => current.filter((passenger) => passenger.id !== passengerId));
      setSeatPassengerIds((current) =>
        Object.fromEntries(
          Object.entries(current).filter(([, assignedPassengerId]) => String(assignedPassengerId) !== String(passengerId)),
        ),
      );
    } catch (deleteError) {
      setCheckoutError(deleteError.message);
    }
  }

  function assignPassenger(tripSeatId, passengerId) {
    setCheckoutError("");
    setSeatPassengerIds((current) => ({ ...current, [tripSeatId]: passengerId }));
  }

  async function refreshAvailability() {
    const refreshedAvailability = await authorizedGet(
      `/api/client/trips/${tripId}/available-seats?startTripStopId=${startTripStopId}&endTripStopId=${endTripStopId}`,
    );
    setAvailability(refreshedAvailability);
  }

  async function createOrder() {
    if (createOrderInFlightRef.current) {
      return;
    }

    setCheckoutError("");

    if (!selectedSeats.length) {
      setCheckoutError("Select one or more available seats.");
      return;
    }

    const missingSeat = selectedSeats.find((seat) => !seatPassengerIds[seat.tripSeatId]);
    if (missingSeat) {
      setCheckoutError(`Choose a passenger for seat ${missingSeat.number}.`);
      return;
    }

    createOrderInFlightRef.current = true;
    setIsCreatingOrder(true);
    try {
      const createdOrder = await authorizedPost("/api/client/orders", {
        tripId: Number(tripId),
        startTripStopId: Number(startTripStopId),
        endTripStopId: Number(endTripStopId),
        seats: selectedSeats.map((seat) => ({
          tripSeatId: seat.tripSeatId,
          passengerId: Number(seatPassengerIds[seat.tripSeatId]),
        })),
      });
      setOrder(createdOrder);
    } catch (createError) {
      setCheckoutError(createError.message);
      if (createError.message?.toLowerCase().includes("seat")) {
        try {
          await refreshAvailability();
        } catch {
          // Keep the original checkout error visible.
        }
      }
    } finally {
      createOrderInFlightRef.current = false;
      setIsCreatingOrder(false);
    }
  }

  async function startPayment() {
    if (!order) {
      return;
    }

    setCheckoutError("");
    setIsStartingPayment(true);
    try {
      const payment = await authorizedPost(`/api/client/orders/${order.id}/payments`, {
        method: paymentMethod,
      });
      navigate(`/payments/${payment.id}`);
    } catch (paymentError) {
      setCheckoutError(paymentError.message);
    } finally {
      setIsStartingPayment(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: "calc(100vh - 64px)",
        py: 5,
        background:
          "radial-gradient(circle at top right, rgba(79,138,91,0.18), transparent 28%), linear-gradient(135deg, #f4f7fb 0%, #edf7f3 100%)",
      }}
    >
      <Container maxWidth="xl">
        <Stack spacing={3}>
          <Card sx={{ bgcolor: "rgba(255,255,255,0.88)", backdropFilter: "blur(12px)" }}>
            <CardContent sx={{ p: { xs: 3, md: 4 } }}>
              <Stack spacing={2.5}>
                <Button
                  variant="text"
                  startIcon={<ArrowBackIcon />}
                  onClick={() => navigate("/trips/search")}
                  sx={{ alignSelf: "flex-start" }}
                >
                  Back to search
                </Button>
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
                      <EventSeatIcon />
                    </Box>
                    <Box>
                      <Typography
                        variant="overline"
                        color="primary.main"
                        sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                      >
                        Seat selection
                      </Typography>
                      <Typography variant="h4">
                        {trip ? `${trip.trainNumber} - ${trip.trainName || "Train"}` : "Select seats"}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {trip ? `${trip.routeNumber} - ${trip.routeName}` : "Loading trip details"}
                      </Typography>
                    </Box>
                  </Stack>
                  {selectedSeats.length ? (
                    <Chip color="primary" label={`${selectedSeats.length} selected`} sx={{ fontWeight: 700 }} />
                  ) : null}
                </Stack>
              </Stack>
            </CardContent>
          </Card>

          {error ? <Alert severity="error">{error}</Alert> : null}

          {isLoading ? (
            <Card>
              <CardContent sx={{ py: 8, display: "grid", placeItems: "center" }}>
                <Stack spacing={2} alignItems="center">
                  <CircularProgress />
                  <Typography color="text.secondary">Loading seats...</Typography>
                </Stack>
              </CardContent>
            </Card>
          ) : null}

          {!isLoading && trip && availability ? (
            <Box
              sx={{
                display: "grid",
                gap: 3,
                gridTemplateColumns: { xs: "1fr", lg: "minmax(0, 1fr) 360px" },
                alignItems: "start",
              }}
            >
              <Stack spacing={3}>
                <Card>
                  <CardContent sx={{ p: 3 }}>
                    <Stack spacing={2}>
                      <Stack direction="row" spacing={1.5} alignItems="center">
                        <TrainIcon color="primary" />
                        <Typography variant="h5">Your trip</Typography>
                      </Stack>

                      <Box
                        sx={{
                          display: "grid",
                          gridTemplateColumns: { xs: "1fr", md: "1fr auto 1fr" },
                          gap: 2,
                          alignItems: "center",
                        }}
                      >
                        <Paper variant="outlined" sx={{ p: 2 }}>
                          <Typography variant="caption" color="text.secondary">
                            Departure
                          </Typography>
                          <Typography variant="h6">{segment.start?.stationName}</Typography>
                          <Typography variant="body2">{formatDateTime(segment.start?.departureAtUtc)}</Typography>
                        </Paper>
                        <Chip variant="outlined" label={`${trip.carCount} cars / ${trip.seatCount} seats`} />
                        <Paper variant="outlined" sx={{ p: 2, textAlign: { xs: "left", md: "right" } }}>
                          <Typography variant="caption" color="text.secondary">
                            Arrival
                          </Typography>
                          <Typography variant="h6">{segment.end?.stationName}</Typography>
                          <Typography variant="body2">{formatDateTime(segment.end?.arrivalAtUtc)}</Typography>
                        </Paper>
                      </Box>
                    </Stack>
                  </CardContent>
                </Card>

                {availability.cars.map((car) => (
                  <Card key={car.tripCarId}>
                    <CardContent sx={{ p: 3 }}>
                      <Stack spacing={2}>
                        <Stack
                          direction={{ xs: "column", sm: "row" }}
                          justifyContent="space-between"
                          alignItems={{ xs: "flex-start", sm: "center" }}
                          spacing={1.5}
                        >
                          <Box>
                            <Typography variant="h6">
                              Car {car.ordinal}: {car.carNumber}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              {car.carTypeCode}
                            </Typography>
                          </Box>
                          <Stack direction="row" spacing={1} flexWrap="wrap">
                            {car.accessible ? <Chip size="small" color="success" label="Accessible" /> : null}
                            <Chip
                              size="small"
                              variant="outlined"
                              label={`${car.seats.filter((seat) => seat.available).length} available`}
                            />
                          </Stack>
                        </Stack>

                        {renderSeatScheme(car, selectedTripSeatIds, toggleSeat)}
                      </Stack>
                    </CardContent>
                  </Card>
                ))}
              </Stack>

              <Card sx={{ position: { lg: "sticky" }, top: { lg: 88 } }}>
                <CardContent sx={{ p: 3 }}>
                  <Stack spacing={2.5}>
                    <Box>
                      <Typography variant="h5">Checkout</Typography>
                    </Box>

                    {checkoutError ? <Alert severity="error">{checkoutError}</Alert> : null}
                    {order?.status === "PAID" ? <Alert severity="success">Payment successful. Tickets are confirmed.</Alert> : null}

                    {!selectedSeats.length ? <Alert severity="info">Select one or more available seats.</Alert> : null}

                    {!order ? (
                      <>
                        <PassengerManager
                          passengers={passengers}
                          benefits={benefits}
                          isLoading={isLoading}
                          error=""
                          onCreate={createPassenger}
                          onDelete={deletePassenger}
                          title="Passengers"
                          description="Choose passengers for the selected seats."
                        />

                        {selectedSeats.length ? (
                          <Stack spacing={1.5}>
                            <Divider />
                            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                              Seat assignment
                            </Typography>
                            {selectedSeats.map((seat) => (
                              <Paper key={seat.tripSeatId} variant="outlined" sx={{ p: 1.5 }}>
                                <Stack spacing={1.25}>
                                  <Box>
                                    <Typography variant="subtitle2">
                                      Car {seat.carOrdinal}, seat {seat.number}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                      {seat.seatTypeCode}
                                    </Typography>
                                  </Box>
                                  <FormControl fullWidth size="small" required disabled={!passengers.length}>
                                    <InputLabel id={`seat-passenger-${seat.tripSeatId}`}>Passenger</InputLabel>
                                    <Select
                                      labelId={`seat-passenger-${seat.tripSeatId}`}
                                      label="Passenger"
                                      value={seatPassengerIds[seat.tripSeatId] ?? ""}
                                      onChange={(event) => assignPassenger(seat.tripSeatId, event.target.value)}
                                    >
                                      {passengers.map((passenger) => (
                                        <MenuItem
                                          key={passenger.id}
                                          value={String(passenger.id)}
                                          disabled={Object.entries(seatPassengerIds).some(
                                            ([assignedSeatId, assignedPassengerId]) =>
                                              String(assignedSeatId) !== String(seat.tripSeatId) &&
                                              String(assignedPassengerId) === String(passenger.id),
                                          )}
                                        >
                                          {passenger.firstName} {passenger.lastName}
                                          {passenger.benefitCode ? ` - ${passenger.benefitCode}` : ""}
                                        </MenuItem>
                                      ))}
                                    </Select>
                                  </FormControl>
                                </Stack>
                              </Paper>
                            ))}
                          </Stack>
                        ) : null}

                        <Button
                          variant="contained"
                          size="large"
                          onClick={createOrder}
                          disabled={!selectedSeats.length || !passengers.length || isCreatingOrder}
                        >
                          {isCreatingOrder ? "Creating order..." : "Create order"}
                        </Button>
                      </>
                    ) : null}

                    {order ? (
                      <Stack spacing={2}>
                        <Divider />
                        <Box>
                          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                            Order #{order.id}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            Status: {order.status}
                          </Typography>
                        </Box>

                        <Stack spacing={1}>
                          {order.tickets.map((ticket) => (
                            <Paper key={ticket.id} variant="outlined" sx={{ p: 1.5 }}>
                              <Stack spacing={0.5}>
                                <Typography variant="subtitle2">
                                  {ticket.passengerFirstName} {ticket.passengerLastName}
                                </Typography>
                                <Typography variant="body2">
                                  Car {ticket.carOrdinal}, seat {ticket.seatNumber}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  {ticket.appliedBenefitCode
                                    ? `${ticket.appliedBenefitCode} - ${ticket.appliedBenefitPercent}% discount`
                                    : "No benefit"}
                                </Typography>
                                <Typography variant="subtitle2">
                                  {formatMoney(ticket.price, ticket.currencyCode)}
                                </Typography>
                              </Stack>
                            </Paper>
                          ))}
                        </Stack>

                        <Box>
                          <Typography variant="body2" color="text.secondary">
                            Total
                          </Typography>
                          <Typography variant="h5">
                            {formatMoney(order.totalAmount, order.currencyCode)}
                          </Typography>
                        </Box>

                        {order.status === "PENDING" ? (
                          <>
                            <FormControl fullWidth>
                              <InputLabel id="payment-method-label">Payment method</InputLabel>
                              <Select
                                labelId="payment-method-label"
                                label="Payment method"
                                value={paymentMethod}
                                onChange={(event) => setPaymentMethod(event.target.value)}
                              >
                                <MenuItem value="CARD">Card</MenuItem>
                                <MenuItem value="APPLE_PAY">Apple Pay</MenuItem>
                                <MenuItem value="GOOGLE_PAY">Google Pay</MenuItem>
                              </Select>
                            </FormControl>

                            <Button
                              variant="contained"
                              size="large"
                              startIcon={<PaymentIcon />}
                              onClick={startPayment}
                              disabled={isStartingPayment}
                            >
                              {isStartingPayment ? "Opening payment..." : "Pay"}
                            </Button>
                          </>
                        ) : null}
                      </Stack>
                    ) : null}
                  </Stack>
                </CardContent>
              </Card>
            </Box>
          ) : null}
        </Stack>
      </Container>
    </Box>
  );
}
