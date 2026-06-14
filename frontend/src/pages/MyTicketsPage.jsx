import ConfirmationNumberIcon from "@mui/icons-material/ConfirmationNumber";
import EventAvailableIcon from "@mui/icons-material/EventAvailable";
import HistoryIcon from "@mui/icons-material/History";
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
  Paper,
  Stack,
  Tab,
  Tabs,
  Typography,
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

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

function formatMoney(amount, currencyCode) {
  const value = Number(amount ?? 0);
  return `${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currencyCode ?? ""}`.trim();
}

function isActiveTicket(ticket, now) {
  if (ticket.status !== "ACTIVE") {
    return false;
  }

  const arrivalTime = new Date(ticket.arrivalAtUtc).getTime();
  return !Number.isNaN(arrivalTime) && arrivalTime >= now.getTime();
}

function statusColor(status) {
  if (status === "ACTIVE") {
    return "success";
  }
  if (status === "CANCELLED" || status === "RETURNED") {
    return "error";
  }
  return "default";
}

function compareByDepartureAsc(left, right) {
  return new Date(left.departureAtUtc).getTime() - new Date(right.departureAtUtc).getTime();
}

function compareByDepartureDesc(left, right) {
  return compareByDepartureAsc(right, left);
}

function TicketCard({ ticket }) {
  return (
    <Paper variant="outlined" sx={{ p: { xs: 2, md: 2.5 }, bgcolor: "rgba(255,255,255,0.86)" }}>
      <Stack spacing={2}>
        <Stack
          direction={{ xs: "column", md: "row" }}
          justifyContent="space-between"
          alignItems={{ xs: "flex-start", md: "center" }}
          spacing={1.5}
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
                {ticket.trainNumber} - {ticket.trainName || "Train"}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {ticket.routeNumber} - {ticket.routeName}
              </Typography>
            </Box>
          </Stack>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <Chip color={statusColor(ticket.status)} label={ticket.status} size="small" />
            <Chip variant="outlined" label={`Ticket #${ticket.id}`} size="small" />
          </Stack>
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
              {ticket.startStationName}
            </Typography>
            <Typography variant="body2">{formatDateTime(ticket.departureAtUtc)}</Typography>
          </Box>
          <Chip variant="outlined" label={`Car ${ticket.carOrdinal}, seat ${ticket.seatNumber}`} />
          <Box sx={{ textAlign: { xs: "left", md: "right" } }}>
            <Typography variant="caption" color="text.secondary">
              Arrival
            </Typography>
            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
              {ticket.endStationName}
            </Typography>
            <Typography variant="body2">{formatDateTime(ticket.arrivalAtUtc)}</Typography>
          </Box>
        </Box>

        <Divider />

        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr", md: "1.2fr 1fr 1fr" },
            gap: 2,
          }}
        >
          <Box>
            <Typography variant="caption" color="text.secondary">
              Passenger
            </Typography>
            <Typography variant="body1">
              {ticket.passengerFirstName} {ticket.passengerLastName}
            </Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">
              Seat type
            </Typography>
            <Typography variant="body1">{ticket.seatTypeCode}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">
              Price
            </Typography>
            <Typography variant="body1" sx={{ fontWeight: 700 }}>
              {formatMoney(ticket.price, ticket.currencyCode)}
            </Typography>
          </Box>
        </Box>

        {ticket.appliedBenefitCode ? (
          <Alert severity="info" sx={{ py: 0.5 }}>
            {ticket.appliedBenefitCode} benefit applied: {ticket.appliedBenefitPercent}% discount
          </Alert>
        ) : null}
      </Stack>
    </Paper>
  );
}

export function MyTicketsPage() {
  const { authorizedGet, user } = useAuth();
  const navigate = useNavigate();
  const [tickets, setTickets] = useState([]);
  const [tab, setTab] = useState("active");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const now = useMemo(() => new Date(), [tickets]);

  useEffect(() => {
    let isActive = true;

    async function loadTickets() {
      setIsLoading(true);
      setError("");

      try {
        const data = await authorizedGet("/api/client/tickets?page=0&size=100");
        if (!isActive) {
          return;
        }

        setTickets(data.content ?? []);
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

    loadTickets();

    return () => {
      isActive = false;
    };
  }, []);

  const activeTickets = useMemo(
    () => tickets.filter((ticket) => isActiveTicket(ticket, now)).sort(compareByDepartureAsc),
    [tickets, now],
  );
  const historyTickets = useMemo(
    () => tickets.filter((ticket) => !isActiveTicket(ticket, now)).sort(compareByDepartureDesc),
    [tickets, now],
  );
  const visibleTickets = tab === "active" ? activeTickets : historyTickets;

  return (
    <Box
      sx={{
        minHeight: "calc(100vh - 64px)",
        py: 5,
        background:
          "radial-gradient(circle at top left, rgba(79,138,91,0.18), transparent 28%), linear-gradient(135deg, #f4f7fb 0%, #edf7f3 100%)",
      }}
    >
      <Container maxWidth="lg">
        <Stack spacing={3}>
          <Card sx={{ bgcolor: "rgba(255,255,255,0.88)", backdropFilter: "blur(12px)" }}>
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
                    <ConfirmationNumberIcon />
                  </Box>
                  <Box>
                    <Typography
                      variant="overline"
                      color="primary.main"
                      sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                    >
                      Tickets
                    </Typography>
                    <Typography variant="h4">My tickets</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {user?.email ?? "Client"}
                    </Typography>
                  </Box>
                </Stack>
                <Button variant="contained" onClick={() => navigate("/trips/search")}>
                  Search trips
                </Button>
              </Stack>
            </CardContent>
          </Card>

          {error ? <Alert severity="error">{error}</Alert> : null}

          <Card>
            <CardContent sx={{ p: { xs: 2, md: 3 } }}>
              <Stack spacing={2.5}>
                <Tabs value={tab} onChange={(_, nextTab) => setTab(nextTab)} variant="scrollable" allowScrollButtonsMobile>
                  <Tab
                    icon={<EventAvailableIcon />}
                    iconPosition="start"
                    value="active"
                    label={`Active (${activeTickets.length})`}
                  />
                  <Tab
                    icon={<HistoryIcon />}
                    iconPosition="start"
                    value="history"
                    label={`History (${historyTickets.length})`}
                  />
                </Tabs>

                {isLoading ? (
                  <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                    <Stack spacing={2} alignItems="center">
                      <CircularProgress />
                      <Typography color="text.secondary">Loading tickets...</Typography>
                    </Stack>
                  </Box>
                ) : null}

                {!isLoading && visibleTickets.length === 0 ? (
                  <Alert severity="info">
                    {tab === "active"
                      ? "No active tickets yet. Paid upcoming trips will appear here."
                      : "No ticket history yet."}
                  </Alert>
                ) : null}

                {!isLoading && visibleTickets.length > 0 ? (
                  <Stack spacing={2}>
                    {visibleTickets.map((ticket) => (
                      <TicketCard key={ticket.id} ticket={ticket} />
                    ))}
                  </Stack>
                ) : null}
              </Stack>
            </CardContent>
          </Card>
        </Stack>
      </Container>
    </Box>
  );
}
