import CreditCardIcon from "@mui/icons-material/CreditCard";
import LockIcon from "@mui/icons-material/Lock";
import PaymentIcon from "@mui/icons-material/Payment";
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
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function formatMoney(amount, currencyCode) {
  const value = Number(amount ?? 0);
  return `${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currencyCode ?? ""}`.trim();
}

function statusColor(status) {
  if (status === "SUCCESSFUL" || status === "PAID") {
    return "success";
  }
  if (status === "FAILED") {
    return "error";
  }
  return "warning";
}

export function PaymentPage() {
  const { paymentId } = useParams();
  const navigate = useNavigate();
  const { authorizedGet, authorizedPost } = useAuth();
  const [payment, setPayment] = useState(null);
  const [order, setOrder] = useState(null);
  const [cardNumber, setCardNumber] = useState("");
  const [cardHolder, setCardHolder] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isPaying, setIsPaying] = useState(false);
  const [isRetrying, setIsRetrying] = useState(false);

  useEffect(() => {
    let isActive = true;

    async function loadPayment() {
      setIsLoading(true);
      setError("");

      try {
        const loadedPayment = await authorizedGet(`/api/client/orders/payments/${paymentId}`);
        if (!isActive) {
          return;
        }
        setPayment(loadedPayment);
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

    loadPayment();

    return () => {
      isActive = false;
    };
  }, [paymentId]);

  async function completePayment(event) {
    event.preventDefault();
    setError("");
    setIsPaying(true);

    try {
      const completedOrder = await authorizedPost(`/api/client/orders/payments/${paymentId}/complete`, {
        cardNumber,
      });
      setOrder(completedOrder);
      setPayment(completedOrder.payments?.find((item) => String(item.id) === String(paymentId)) ?? payment);
    } catch (paymentError) {
      setError(paymentError.message);
    } finally {
      setIsPaying(false);
    }
  }

  async function retryPayment() {
    if (!payment?.orderId) {
      return;
    }

    setError("");
    setIsRetrying(true);
    try {
      const nextPayment = await authorizedPost(`/api/client/orders/${payment.orderId}/payments`, {
        method: payment.method,
      });
      navigate(`/payments/${nextPayment.id}`, { replace: true });
    } catch (retryError) {
      setError(retryError.message);
    } finally {
      setIsRetrying(false);
    }
  }

  const currentStatus = order?.status === "PAID" ? "PAID" : payment?.status;
  const canPay = payment?.status === "PROCESSING" && order?.status !== "PAID";

  return (
    <Box
      sx={{
        minHeight: "calc(100vh - 64px)",
        py: 5,
        background:
          "radial-gradient(circle at top right, rgba(79,138,91,0.18), transparent 28%), linear-gradient(135deg, #e8eef2 0%, #dde9e3 100%)",
      }}
    >
      <Container maxWidth="md">
        <Stack spacing={3}>
          <Card sx={{ bgcolor: "rgba(255,255,255,0.9)", backdropFilter: "blur(12px)" }}>
            <CardContent sx={{ p: { xs: 3, md: 4 } }}>
              <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems={{ xs: "flex-start", sm: "center" }}>
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
                  <LockIcon />
                </Box>
                <Box sx={{ flexGrow: 1 }}>
                  <Typography variant="overline" color="primary.main" sx={{ letterSpacing: "0.16em", fontWeight: 700 }}>
                    Secure payment
                  </Typography>
                  <Typography variant="h4">Complete your payment</Typography>
                </Box>
                {currentStatus ? <Chip color={statusColor(currentStatus)} label={currentStatus} /> : null}
              </Stack>
            </CardContent>
          </Card>

          {error ? <Alert severity="error">{error}</Alert> : null}

          {isLoading ? (
            <Card>
              <CardContent sx={{ py: 8, display: "grid", placeItems: "center" }}>
                <Stack spacing={2} alignItems="center">
                  <CircularProgress />
                  <Typography color="text.secondary">Loading payment...</Typography>
                </Stack>
              </CardContent>
            </Card>
          ) : null}

          {!isLoading && payment ? (
            <Card>
              <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                <Stack spacing={3} component="form" onSubmit={completePayment}>
                  <Stack direction="row" spacing={2} alignItems="center">
                    <PaymentIcon color="primary" />
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        Amount
                      </Typography>
                      <Typography variant="h4">{formatMoney(payment.amount, payment.currencyCode)}</Typography>
                    </Box>
                  </Stack>

                  <Divider />

                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Payment method
                    </Typography>
                    <Typography variant="h6">{payment.method}</Typography>
                  </Box>

                  {canPay ? (
                    <>
                      <TextField
                        label="Card number"
                        value={cardNumber}
                        onChange={(event) => setCardNumber(event.target.value)}
                        placeholder="4242 4242 4242 4242"
                        fullWidth
                      />
                      <TextField
                        label="Cardholder name"
                        value={cardHolder}
                        onChange={(event) => setCardHolder(event.target.value)}
                        placeholder="Name on card"
                        fullWidth
                      />
                      <Button type="submit" variant="contained" size="large" startIcon={<CreditCardIcon />} disabled={isPaying}>
                        {isPaying ? "Processing..." : "Pay"}
                      </Button>
                    </>
                  ) : null}

                  {order?.status === "PAID" ? (
                    <Stack spacing={2}>
                      <Alert severity="success">Payment completed. Your tickets are confirmed.</Alert>
                      <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5}>
                        <Button variant="contained" onClick={() => navigate("/tickets")}>
                          View tickets
                        </Button>
                        <Button variant="outlined" onClick={() => navigate("/orders")}>
                          View orders
                        </Button>
                        <Button variant="outlined" onClick={() => navigate("/trips/search")}>
                          Back to search
                        </Button>
                      </Stack>
                    </Stack>
                  ) : null}

                  {payment.status === "FAILED" ? (
                    <Stack spacing={2}>
                      <Alert severity="warning">The payment was not completed. You can try again while the order is active.</Alert>
                      <Button variant="contained" onClick={retryPayment} disabled={isRetrying}>
                        {isRetrying ? "Opening payment..." : "Try again"}
                      </Button>
                    </Stack>
                  ) : null}
                </Stack>
              </CardContent>
            </Card>
          ) : null}
        </Stack>
      </Container>
    </Box>
  );
}
