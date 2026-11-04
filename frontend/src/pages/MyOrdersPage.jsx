import ReceiptLongIcon from "@mui/icons-material/ReceiptLong";
import TrainIcon from "@mui/icons-material/Train";
import PaymentIcon from "@mui/icons-material/Payment";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
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
  Typography,
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useI18n } from "../i18n/I18nContext.jsx";

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

function statusColor(status) {
  if (status === "PAID" || status === "SUCCESSFUL" || status === "ACTIVE") {
    return "success";
  }
  if (status === "CANCELLED" || status === "EXPIRED" || status === "FAILED") {
    return "error";
  }
  return "warning";
}

function paymentSummary(order) {
  const lastPayment = order.payments?.at(-1);
  return lastPayment?.status ?? "NO_PAYMENT";
}

function OrderTicketSummary({ ticket, codeLabel, t }) {
  return (
    <Paper variant="outlined" sx={{ p: 1.5 }}>
      <Stack spacing={1}>
        <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1}>
          <Box>
            <Typography variant="subtitle2">
              {ticket.passengerFirstName} {ticket.passengerLastName}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {ticket.startStationName}{" -> "}{ticket.endStationName}
            </Typography>
          </Box>
          <Chip size="small" color={statusColor(ticket.status)} label={codeLabel("status", ticket.status)} />
        </Stack>
        <Typography variant="body2">
          {formatDateTime(ticket.departureAtUtc)} - {t("Car").toLowerCase()} {ticket.carOrdinal}, {t("Seat").toLowerCase()} {ticket.seatNumber}
        </Typography>
        <Typography variant="subtitle2">{formatMoney(ticket.price, ticket.currencyCode)}</Typography>
      </Stack>
    </Paper>
  );
}

export function MyOrdersPage() {
  const { authorizedGet, authorizedPost, user } = useAuth();
  const { codeLabel, t } = useI18n();
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [openingPaymentOrderId, setOpeningPaymentOrderId] = useState(null);

  useEffect(() => {
    let isActive = true;

    async function loadOrders() {
      setIsLoading(true);
      setError("");

      try {
        const data = await authorizedGet("/api/client/orders?page=0&size=100");
        if (!isActive) {
          return;
        }

        setOrders(data.content ?? []);
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

    loadOrders();

    return () => {
      isActive = false;
    };
  }, []);

  function canPayOrder(order) {
    if (order.status !== "PENDING") {
      return false;
    }

    const expiresAt = new Date(order.expiresAtUtc);
    return Number.isNaN(expiresAt.getTime()) || expiresAt > new Date();
  }

  async function openPayment(order) {
    setError("");
    setOpeningPaymentOrderId(order.id);

    try {
      const processingPayment = order.payments?.find((payment) => payment.status === "PROCESSING");
      if (processingPayment) {
        navigate(`/payments/${processingPayment.id}`);
        return;
      }

      const payment = await authorizedPost(`/api/client/orders/${order.id}/payments`, {
        method: "CARD",
      });
      navigate(`/payments/${payment.id}`);
    } catch (paymentError) {
      setError(paymentError.message);
    } finally {
      setOpeningPaymentOrderId(null);
    }
  }

  return (
    <Box
      sx={{
        minHeight: "calc(100vh - 64px)",
        py: 5,
        background:
          "radial-gradient(circle at top right, rgba(79,138,91,0.18), transparent 28%), linear-gradient(135deg, #e8eef2 0%, #dde9e3 100%)",
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
                    <ReceiptLongIcon />
                  </Box>
                  <Box>
                    <Typography
                      variant="overline"
                      color="primary.main"
                      sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                    >
                      {t("Orders")}
                    </Typography>
                    <Typography variant="h4">{t("My orders")}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {user?.email ?? t("Client")}
                    </Typography>
                  </Box>
                </Stack>
                <Button variant="contained" onClick={() => navigate("/trips/search")}>
                  {t("Search trips")}
                </Button>
              </Stack>
            </CardContent>
          </Card>

          {error ? <Alert severity="error">{error}</Alert> : null}

          <Card>
            <CardContent sx={{ p: { xs: 2, md: 3 } }}>
              {isLoading ? (
                <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                  <Stack spacing={2} alignItems="center">
                    <CircularProgress />
                    <Typography color="text.secondary">{t("Loading orders...")}</Typography>
                  </Stack>
                </Box>
              ) : null}

              {!isLoading && orders.length === 0 ? (
                <Alert severity="info">{t("No orders yet. Created and paid bookings will appear here.")}</Alert>
              ) : null}

              {!isLoading && orders.length > 0 ? (
                <Stack spacing={1.5}>
                  {orders.map((order) => (
                    <Accordion key={order.id} disableGutters>
                      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <Stack
                          direction={{ xs: "column", md: "row" }}
                          spacing={1.5}
                          alignItems={{ xs: "flex-start", md: "center" }}
                          sx={{ width: "100%", pr: 2 }}
                        >
                          <Stack direction="row" spacing={1.25} alignItems="center" sx={{ flexGrow: 1 }}>
                            <TrainIcon color="primary" />
                            <Box>
                              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                                {t("Order #{id}", { id: order.id })}
                              </Typography>
                              <Typography variant="body2" color="text.secondary">
                                {formatDateTime(order.createdAtUtc)} - {t("{count} tickets", { count: order.tickets?.length ?? 0 })}
                              </Typography>
                            </Box>
                          </Stack>
                          <Chip size="small" color={statusColor(order.status)} label={codeLabel("status", order.status)} />
                          <Chip size="small" variant="outlined" label={t("Payment: {status}", { status: codeLabel("status", paymentSummary(order)) })} />
                          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                            {formatMoney(order.totalAmount, order.currencyCode)}
                          </Typography>
                        </Stack>
                      </AccordionSummary>
                      <AccordionDetails>
                        <Stack spacing={2}>
                          <Divider />
                          {order.expiresAtUtc ? (
                            <Typography variant="body2" color="text.secondary">
                              {t("Payment deadline: {date}", { date: formatDateTime(order.expiresAtUtc) })}
                            </Typography>
                          ) : null}
                          <Stack spacing={1.25}>
                            {(order.tickets ?? []).map((ticket) => (
                              <OrderTicketSummary key={ticket.id} ticket={ticket} codeLabel={codeLabel} t={t} />
                            ))}
                          </Stack>
                          {order.status === "PENDING" ? (
                            <Stack spacing={1.5}>
                              <Alert severity="warning">{t("This order is reserved until the payment deadline.")}</Alert>
                              {canPayOrder(order) ? (
                                <Button
                                  variant="contained"
                                  startIcon={<PaymentIcon />}
                                  onClick={() => openPayment(order)}
                                  disabled={openingPaymentOrderId === order.id}
                                  sx={{ alignSelf: "flex-start" }}
                                >
                                  {openingPaymentOrderId === order.id ? t("Opening payment...") : t("Pay order")}
                                </Button>
                              ) : null}
                            </Stack>
                          ) : null}
                        </Stack>
                      </AccordionDetails>
                    </Accordion>
                  ))}
                </Stack>
              ) : null}
            </CardContent>
          </Card>
        </Stack>
      </Container>
    </Box>
  );
}
