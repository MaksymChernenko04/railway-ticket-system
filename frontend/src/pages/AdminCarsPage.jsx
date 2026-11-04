import AccessibleIcon from "@mui/icons-material/Accessible";
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
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  IconButton,
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
  Tooltip,
  Typography,
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { nextSortState, SortableTableCell, sortRows } from "../components/SortableTableCell.jsx";
import { useI18n } from "../i18n/I18nContext.jsx";

const emptyCarForm = {
  typeCode: "",
  number: "",
  accessible: false,
};

const emptySeatForm = {
  typeCode: "",
  number: "",
  row: "",
  column: "",
  direction: "FORWARD",
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

function renderSeatDeleteButton(seat, onDeleteSeat) {
  return (
    <IconButton
      size="small"
      color="error"
      onClick={() => onDeleteSeat(seat.id)}
      sx={{
        position: "absolute",
        top: -8,
        right: -8,
        width: 22,
        height: 22,
        bgcolor: "background.paper",
        border: "1px solid rgba(211,47,47,0.24)",
        "&:hover": { bgcolor: "error.light", color: "common.white" },
      }}
    >
      <DeleteIcon sx={{ fontSize: 14 }} />
    </IconButton>
  );
}

function renderBerth(seat, level, onDeleteSeat, t, codeLabel, orientation = "sideways") {
  if (seat) {
    return (
      <Tooltip
        title={`${t("Seat")} ${seat.number}, ${codeLabel("seatType", seat.typeCode)}, ${t(level)}, ${codeLabel("direction", seat.direction)}`}
      >
        <Box sx={berthSx(level, orientation)}>
          <Typography variant="caption" sx={{ fontWeight: 800, lineHeight: 1 }}>
            {seat.number}
          </Typography>
          {renderSeatDeleteButton(seat, onDeleteSeat)}
        </Box>
      </Tooltip>
    );
  }

  return <Box sx={berthPlaceholderSx(orientation)} />;
}

function renderSeatPlace(seat, onDeleteSeat, t, codeLabel) {
  if (seat) {
    return (
      <Tooltip title={`${t("Seat")} ${seat.number}, ${codeLabel("seatType", seat.typeCode)}, ${codeLabel("direction", seat.direction)}`}>
        <Box sx={seatSx(seat.direction)}>
          <Typography variant="caption" sx={{ fontWeight: 800, lineHeight: 1 }}>
            {seat.number}
          </Typography>
          {renderSeatDeleteButton(seat, onDeleteSeat)}
        </Box>
      </Tooltip>
    );
  }

  return (
    <Box
      sx={{
        width: 44,
        height: 44,
        borderRadius: 2,
        border: "1px dashed rgba(88,96,92,0.34)",
        bgcolor: "rgba(112,126,136,0.10)",
      }}
    />
  );
}

function seatSx(direction) {
  const shadowByDirection = {
    FORWARD: "inset 6px 0 0 rgba(79,138,91,0.36)",
    BACKWARD: "inset -6px 0 0 rgba(79,138,91,0.36)",
    SIDEWAYS: "inset 0 6px 0 rgba(79,138,91,0.36)",
  };

  return {
    width: 44,
    height: 44,
    borderRadius: 2,
    border: "1px solid rgba(47,99,57,0.44)",
    bgcolor: "rgba(255,255,255,0.92)",
    display: "grid",
    placeItems: "center",
    position: "relative",
    boxShadow: shadowByDirection[direction] ?? shadowByDirection.FORWARD,
  };
}

function berthSx(level, orientation) {
  const isLong = orientation === "forward";

  return {
    width: isLong ? 106 : 46,
    height: isLong ? 34 : 62,
    borderRadius: 2,
    border: "1px solid rgba(47,99,57,0.44)",
    bgcolor: level === "upper" ? "rgba(94, 122, 176, 0.20)" : "rgba(255,255,255,0.92)",
    display: "grid",
    placeItems: "center",
    position: "relative",
    boxShadow:
      level === "upper" ? "inset 0 5px 0 rgba(38,62,133,0.55)" : "inset 0 -5px 0 rgba(79,138,91,0.32)",
  };
}

function berthPlaceholderSx(orientation) {
  const isLong = orientation === "forward";

  return {
    width: isLong ? 106 : 46,
    height: isLong ? 34 : 62,
    borderRadius: 2,
    border: "1px dashed rgba(88,96,92,0.34)",
    bgcolor: "rgba(112,126,136,0.10)",
  };
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

function renderCoupeBlock(rowNumber, rowSeats, lux, onDeleteSeat, t, codeLabel) {
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
          {lux ? null : renderBerth(leftUpper, "upper", onDeleteSeat, t, codeLabel)}
          {renderBerth(leftLower, "lower", onDeleteSeat, t, codeLabel)}
        </Stack>
        {renderCorridor({ width: 18, height: lux ? 62 : 126 })}
        <Stack spacing={0.5}>
          {lux ? null : renderBerth(rightUpper, "upper", onDeleteSeat, t, codeLabel)}
          {renderBerth(rightLower, "lower", onDeleteSeat, t, codeLabel)}
        </Stack>
      </Box>
    </Stack>
  );
}

function renderSleeperScheme(seats, carType, onDeleteSeat, t, codeLabel) {
  const rows = groupSeatsByRow(seats);
  const isLux = carType === "LUX_SLEEPER";
  const isOpen = carType === "OPEN_SLEEPER";

  return (
    <Box sx={{ minWidth: Math.max(760, rows.length * 138), pb: 1 }}>
      <Stack direction="row" spacing={1.5} alignItems="flex-start">
        {rows.map(([row, rowSeats]) => (
          <Stack key={row} spacing={0.75} alignItems="center">
            {renderCoupeBlock(row, rowSeats, isLux, onDeleteSeat, t, codeLabel)}
            {isOpen ? (
              <>
                {renderCorridor({ width: 126, height: 22 })}
                <Stack spacing={0.5}>
                  {renderBerth(getSeatByColumn(rowSeats, 6), "upper", onDeleteSeat, t, codeLabel, "forward")}
                  {renderBerth(getSeatByColumn(rowSeats, 5), "lower", onDeleteSeat, t, codeLabel, "forward")}
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

function renderFirstClassRow(rowSeats, onDeleteSeat, t, codeLabel) {
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
        <Box key={seat?.id ?? `left-empty-${index}`}>{renderSeatPlace(seat, onDeleteSeat, t, codeLabel)}</Box>
      ))}
      {renderCorridor({ width: 44, height: 22 })}
      {rightColumns.map((seat, index) => (
        <Box key={seat?.id ?? `right-empty-${index}`}>{renderSeatPlace(seat, onDeleteSeat, t, codeLabel)}</Box>
      ))}
    </Stack>
  );
}

function renderSittingScheme(seats, carType, onDeleteSeat, t, codeLabel) {
  const rows = groupSeatsByRow(seats);
  const isFirstClass = carType === "SITTING_CLASS_1";

  return (
    <Box sx={{ minWidth: Math.max(760, rows.length * (isFirstClass ? 86 : 92)), pb: 1 }}>
      <Stack direction="row" spacing={1.5} alignItems="flex-start">
        {rows.map(([row, rowSeats]) => (
          <Stack key={row} spacing={0.75} alignItems="center">
            <Typography variant="caption" color="text.secondary">
              {t("Row")} {row}
            </Typography>
            {isFirstClass ? (
              renderFirstClassRow(rowSeats, onDeleteSeat, t, codeLabel)
            ) : (
              <Stack spacing={0.5} alignItems="center">
                {renderSeatPlace(getSeatByColumn(rowSeats, 1), onDeleteSeat, t, codeLabel)}
                {renderSeatPlace(getSeatByColumn(rowSeats, 2), onDeleteSeat, t, codeLabel)}
                {renderCorridor({ width: 44, height: 22 })}
                {renderSeatPlace(getSeatByColumn(rowSeats, 3), onDeleteSeat, t, codeLabel)}
                {renderSeatPlace(getSeatByColumn(rowSeats, 4), onDeleteSeat, t, codeLabel)}
              </Stack>
            )}
          </Stack>
        ))}
      </Stack>
    </Box>
  );
}

function renderGenericScheme(seats, onDeleteSeat, t, codeLabel) {
  const rows = groupSeatsByRow(seats);

  return (
    <Box sx={{ minWidth: Math.max(620, rows.length * 96), pb: 1 }}>
      <Stack direction="row" spacing={1.2} alignItems="flex-start">
        {rows.map(([row, rowSeats]) => (
          <Stack key={row} spacing={0.75} alignItems="center">
            <Typography variant="caption" color="text.secondary">
              {t("Row")} {row}
            </Typography>
            {rowSeats.map((seat) => (
              <Box key={seat.id}>{renderSeatPlace(seat, onDeleteSeat, t, codeLabel)}</Box>
            ))}
          </Stack>
        ))}
      </Stack>
    </Box>
  );
}

function renderSeatScheme(car, seats, onDeleteSeat, t, codeLabel) {
  const carType = car?.typeCode?.toUpperCase();
  const isSleeper = ["COUPE_SLEEPER", "LUX_SLEEPER", "OPEN_SLEEPER"].includes(carType);
  const isSitting = ["SITTING_CLASS_1", "SITTING_CLASS_2"].includes(carType);

  if (seats.length === 0) {
    return <Alert severity="info">{t("This car has no seats yet.")}</Alert>;
  }

  return (
    <Box
      sx={{
        overflowX: "auto",
        border: "1px solid rgba(47,99,57,0.30)",
        borderRadius: 3,
        bgcolor: "rgba(240,246,241,0.88)",
        p: 2,
      }}
    >
      {isSleeper ? renderSleeperScheme(seats, carType, onDeleteSeat, t, codeLabel) : null}
      {isSitting ? renderSittingScheme(seats, carType, onDeleteSeat, t, codeLabel) : null}
      {isSleeper || isSitting ? null : renderGenericScheme(seats, onDeleteSeat, t, codeLabel)}
    </Box>
  );
}

export function AdminCarsPage() {
  const { authorizedGet, authorizedPost, authorizedDelete, user } = useAuth();
  const { codeLabel, t } = useI18n();
  const [carsPage, setCarsPage] = useState(null);
  const [carForm, setCarForm] = useState(emptyCarForm);
  const [seatForm, setSeatForm] = useState(emptySeatForm);
  const [carTypes, setCarTypes] = useState([]);
  const [seatTypes, setSeatTypes] = useState([]);
  const [selectedCar, setSelectedCar] = useState(null);
  const [seats, setSeats] = useState([]);
  const [feedback, setFeedback] = useState("");
  const [error, setError] = useState("");
  const [seatError, setSeatError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSeatsLoading, setIsSeatsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSeatSubmitting, setIsSeatSubmitting] = useState(false);
  const [pageIndex, setPageIndex] = useState(0);
  const [sort, setSort] = useState({ field: "number", direction: "asc" });

  const isSeatsDialogOpen = Boolean(selectedCar);
  const isCarsLoaded = isLoading === false;
  const hasCars = Boolean(carsPage?.content?.length);
  const sortedCars = useMemo(
    () => sortRows(carsPage?.content ?? [], sort),
    [carsPage, sort],
  );

  async function loadCars(page = pageIndex) {
    setIsLoading(true);
    setError("");

    try {
      const data = await authorizedGet(`/api/admin/cars?page=${page}&size=8&sort=number,asc`);
      setCarsPage(data);
      setPageIndex(page);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setIsLoading(false);
    }
  }

  async function loadTypes() {
    try {
      const [loadedCarTypes, loadedSeatTypes] = await Promise.all([
        authorizedGet("/api/admin/cars/types"),
        authorizedGet("/api/admin/cars/seat-types"),
      ]);
      setCarTypes(loadedCarTypes);
      setSeatTypes(loadedSeatTypes);
    } catch (loadError) {
      setError(loadError.message);
    }
  }

  async function loadSeats(car) {
    setSelectedCar(car);
    setSeats([]);
    setSeatError("");
    setIsSeatsLoading(true);

    try {
      const data = await authorizedGet(`/api/admin/cars/${car.id}/seats`);
      setSeats(data);
    } catch (loadError) {
      setSeatError(loadError.message);
    } finally {
      setIsSeatsLoading(false);
    }
  }

  useEffect(() => {
    loadCars(0);
    loadTypes();
  }, []);

  async function handleCarSubmit(event) {
    event.preventDefault();
    setFeedback("");
    setError("");
    setIsSubmitting(true);

    try {
      await authorizedPost("/api/admin/cars", carForm);
      setFeedback(t("Car created."));
      setCarForm(emptyCarForm);
      await loadCars(0);
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSeatSubmit(event) {
    event.preventDefault();
    if (selectedCar === null) {
      return;
    }

    setFeedback("");
    setSeatError("");
    setIsSeatSubmitting(true);

    try {
      await authorizedPost(`/api/admin/cars/${selectedCar.id}/seats`, {
        ...seatForm,
        row: Number(seatForm.row),
        column: Number(seatForm.column),
      });
      setSeatForm(emptySeatForm);
      await loadSeats(selectedCar);
    } catch (submitError) {
      setSeatError(submitError.message);
    } finally {
      setIsSeatSubmitting(false);
    }
  }

  async function deleteCar(id) {
    if (globalThis.confirm(t("Are you sure you want to delete this car?")) === false) {
      return;
    }

    setFeedback("");
    setError("");

    try {
      await authorizedDelete(`/api/admin/cars/${id}`);
      setFeedback(t("Car deleted."));

      const isLastItemOnPage = carsPage?.content?.length === 1;
      const nextPageIndex = isLastItemOnPage && pageIndex > 0 ? pageIndex - 1 : pageIndex;

      await loadCars(nextPageIndex);
    } catch (deleteError) {
      setError(deleteError.message);
    }
  }

  async function deleteSeat(seatId) {
    if (selectedCar === null || globalThis.confirm(t("Are you sure you want to delete this seat?")) === false) {
      return;
    }

    setSeatError("");

    try {
      await authorizedDelete(`/api/admin/cars/${selectedCar.id}/seats/${seatId}`);
      await loadSeats(selectedCar);
    } catch (deleteError) {
      setSeatError(deleteError.message);
    }
  }

  function closeSeatsDialog() {
    setSelectedCar(null);
    setSeats([]);
    setSeatError("");
    setSeatForm(emptySeatForm);
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
                  <TrainIcon />
                </Box>
                <Box>
                  <Typography
                    variant="overline"
                    color="primary.main"
                    sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                  >
                    {t("Admin panel")}
                  </Typography>
                  <Typography variant="h4">{t("Cars management")}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t("Logged in as {name} ({email})", {
                      name: [user?.firstName, user?.lastName].filter(Boolean).join(" "),
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
              gridTemplateColumns: { xs: "1fr", lg: "400px minmax(0, 1fr)" },
              alignItems: "start",
            }}
          >
            <Card>
              <CardContent sx={{ p: 3 }}>
                <Stack spacing={3} component="form" onSubmit={handleCarSubmit}>
                  <Box>
                    <Typography variant="h5">{t("Create car")}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                      {t("Define a car type, number, and accessibility settings.")}
                    </Typography>
                  </Box>

                  <FormControl fullWidth required>
                    <InputLabel id="car-type-label">{t("Type")}</InputLabel>
                    <Select
                      labelId="car-type-label"
                      label={t("Type")}
                      value={carForm.typeCode}
                      onChange={(event) => setCarForm({ ...carForm, typeCode: event.target.value })}
                    >
                      {carTypes.map((type) => (
                        <MenuItem key={type.code} value={type.code}>
                          {codeLabel("carType", type.code)} x{type.priceMultiplier}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <TextField
                    label={t("Number")}
                    value={carForm.number}
                    onChange={(event) => setCarForm({ ...carForm, number: event.target.value })}
                    slotProps={{ htmlInput: { maxLength: 20 } }}
                    required
                    fullWidth
                  />

                  <FormControlLabel
                    control={
                      <Switch
                        checked={carForm.accessible}
                        onChange={(event) => setCarForm({ ...carForm, accessible: event.target.checked })}
                      />
                    }
                    label={t("Accessible car")}
                  />

                  {error ? <Alert severity="error">{error}</Alert> : null}

                  <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                    {isSubmitting ? t("Saving...") : t("Create car")}
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
                    <Typography variant="h5">{t("Existing cars")}</Typography>
                    <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => loadCars(pageIndex)}>
                      {t("Refresh")}
                    </Button>
                  </Stack>

                  {feedback ? (
                    <Alert severity="success" onClose={() => setFeedback("")}>
                      {feedback}
                    </Alert>
                  ) : null}

                  {isLoading ? (
                    <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                      <Stack spacing={2} alignItems="center">
                        <CircularProgress />
                        <Typography color="text.secondary">{t("Loading cars...")}</Typography>
                      </Stack>
                    </Box>
                  ) : null}

                  {isCarsLoaded && hasCars ? (
                    <>
                      <TableContainer component={Paper} variant="outlined">
                        <Table>
                          <TableHead>
                            <TableRow>
                              <SortableTableCell sort={sort} field="number" onSort={handleSort}>{t("Number")}</SortableTableCell>
                              <SortableTableCell sort={sort} field="typeCode" onSort={handleSort}>{t("Type")}</SortableTableCell>
                              <SortableTableCell sort={sort} field="accessible" onSort={handleSort}>{t("Accessible")}</SortableTableCell>
                              <TableCell>{t("Action")}</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {sortedCars.map((car) => (
                              <TableRow key={car.id} hover>
                                <TableCell>{car.number}</TableCell>
                                <TableCell>{codeLabel("carType", car.typeCode)}</TableCell>
                                <TableCell>
                                  <Chip
                                    size="small"
                                    color={car.accessible ? "success" : "default"}
                                    icon={car.accessible ? <AccessibleIcon /> : undefined}
                                    label={car.accessible ? t("Yes") : t("No")}
                                  />
                                </TableCell>
                                <TableCell>
                                  <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
                                    <Button variant="outlined" onClick={() => loadSeats(car)}>
                                      {t("View seats")}
                                    </Button>
                                    <Button variant="contained" color="error" onClick={() => deleteCar(car.id)}>
                                      {t("Delete")}
                                    </Button>
                                  </Stack>
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
                        <Button variant="outlined" onClick={() => loadCars(pageIndex - 1)} disabled={carsPage.first}>
                          {t("Previous")}
                        </Button>
                        <Typography variant="body2" color="text.secondary">
                          {t("Page {current} of {total}", { current: carsPage.number + 1, total: carsPage.totalPages || 1 })}
                        </Typography>
                        <Button variant="outlined" onClick={() => loadCars(pageIndex + 1)} disabled={carsPage.last}>
                          {t("Next")}
                        </Button>
                      </Stack>
                    </>
                  ) : null}

                  {isCarsLoaded && hasCars === false ? <Alert severity="info">{t("No cars found yet.")}</Alert> : null}
                </Stack>
              </CardContent>
            </Card>
          </Box>
        </Stack>
      </Container>

      <Dialog open={isSeatsDialogOpen} onClose={closeSeatsDialog} fullWidth maxWidth="xl">
        <DialogTitle sx={{ pr: 7 }}>
          <Stack spacing={0.5}>
            <Typography variant="h5">{t("Seats scheme")}</Typography>
            <Typography variant="body2" color="text.secondary">
              {selectedCar ? `${t("Car")} ${selectedCar.number}, ${t("Type").toLowerCase()} ${codeLabel("carType", selectedCar.typeCode)}` : ""}
            </Typography>
          </Stack>
          <IconButton
            aria-label={t("Close seats dialog")}
            onClick={closeSeatsDialog}
            sx={{ position: "absolute", right: 16, top: 16 }}
          >
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent dividers>
          <Stack spacing={3}>
            <Card variant="outlined" sx={{ boxShadow: "none" }}>
              <CardContent sx={{ p: 2.5 }}>
                <Stack
                  spacing={2}
                  component="form"
                  onSubmit={handleSeatSubmit}
                  direction={{ xs: "column", lg: "row" }}
                  alignItems={{ xs: "stretch", lg: "flex-end" }}
                >
                  <FormControl required sx={{ minWidth: 190 }}>
                    <InputLabel id="seat-type-label">{t("Seat type")}</InputLabel>
                    <Select
                      labelId="seat-type-label"
                      label={t("Seat type")}
                      value={seatForm.typeCode}
                      onChange={(event) => setSeatForm({ ...seatForm, typeCode: event.target.value })}
                    >
                      {seatTypes.map((type) => (
                        <MenuItem key={type.code} value={type.code}>
                          {codeLabel("seatType", type.code)} x{type.priceMultiplier}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <TextField
                    label={t("Number")}
                    value={seatForm.number}
                    onChange={(event) => setSeatForm({ ...seatForm, number: event.target.value })}
                    slotProps={{ htmlInput: { maxLength: 10 } }}
                    required
                  />
                  <TextField
                    label={t("Row")}
                    type="number"
                    value={seatForm.row}
                    onChange={(event) => setSeatForm({ ...seatForm, row: event.target.value })}
                    slotProps={{ htmlInput: { min: 1 } }}
                    required
                  />
                  <TextField
                    label={t("Column")}
                    type="number"
                    value={seatForm.column}
                    onChange={(event) => setSeatForm({ ...seatForm, column: event.target.value })}
                    slotProps={{ htmlInput: { min: 1 } }}
                    required
                  />
                  <FormControl sx={{ minWidth: 160 }}>
                    <InputLabel id="seat-direction-label">{t("Direction")}</InputLabel>
                    <Select
                      labelId="seat-direction-label"
                      label={t("Direction")}
                      value={seatForm.direction}
                      onChange={(event) => setSeatForm({ ...seatForm, direction: event.target.value })}
                    >
                      <MenuItem value="FORWARD">{codeLabel("direction", "FORWARD")}</MenuItem>
                      <MenuItem value="BACKWARD">{codeLabel("direction", "BACKWARD")}</MenuItem>
                      <MenuItem value="SIDEWAYS">{codeLabel("direction", "SIDEWAYS")}</MenuItem>
                    </Select>
                  </FormControl>
                  <Button type="submit" variant="contained" disabled={isSeatSubmitting}>
                    {isSeatSubmitting ? t("Saving...") : t("Add seat")}
                  </Button>
                </Stack>
              </CardContent>
            </Card>

            {seatError ? <Alert severity="error">{seatError}</Alert> : null}

            {isSeatsLoading ? (
              <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                <Stack spacing={2} alignItems="center">
                  <CircularProgress />
                  <Typography color="text.secondary">{t("Loading seats...")}</Typography>
                </Stack>
              </Box>
            ) : (
              renderSeatScheme(selectedCar, seats, deleteSeat, t, codeLabel)
            )}
          </Stack>
        </DialogContent>
      </Dialog>
    </Box>
  );
}
