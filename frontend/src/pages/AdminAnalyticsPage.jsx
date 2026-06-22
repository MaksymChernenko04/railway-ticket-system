import AnalyticsIcon from "@mui/icons-material/Analytics";
import RefreshIcon from "@mui/icons-material/Refresh";
import SendIcon from "@mui/icons-material/Send";
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
  Paper,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { nextSortState, SortableTableCell, sortRows } from "../components/SortableTableCell.jsx";

function toDateInput(date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return localDate.toISOString().slice(0, 10);
}

function toStartOfLocalDayIso(dateValue) {
  return new Date(`${dateValue}T00:00:00`).toISOString();
}

function toEndOfLocalDayExclusiveIso(dateValue) {
  const date = new Date(`${dateValue}T00:00:00`);
  date.setDate(date.getDate() + 1);
  return date.toISOString();
}

function buildInitialFilters() {
  const dateTo = new Date();
  const dateFrom = new Date(dateTo);
  dateFrom.setDate(dateFrom.getDate() - 30);

  return {
    dateFrom: toDateInput(dateFrom),
    dateTo: toDateInput(dateTo),
    routeId: "",
    limit: "10",
  };
}

function createConversationId() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }

  return `admin-chat-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatNumber(value, options) {
  if (value === null || value === undefined) {
    return "-";
  }

  return Number(value).toLocaleString(undefined, options);
}

function formatMoney(value, currencyCode) {
  if (value === null || value === undefined) {
    return "-";
  }

  return `${formatNumber(value, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currencyCode}`;
}

function EmptyRows({ colSpan }) {
  return (
    <TableRow>
      <TableCell colSpan={colSpan}>
        <Typography variant="body2" color="text.secondary" align="center" sx={{ py: 3 }}>
          No data for selected filters.
        </Typography>
      </TableCell>
    </TableRow>
  );
}

function AnalyticsTable({ title, children }) {
  return (
    <Card>
      <CardContent sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Typography variant="h6">{title}</Typography>
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">{children}</Table>
          </TableContainer>
        </Stack>
      </CardContent>
    </Card>
  );
}

function SummaryKpiTable({ rows }) {
  const [sort, setSort] = useState({ field: "currencyCode", direction: "asc" });
  const sortedRows = sortRows(rows, sort, {
    soldTicketCount: (row) => Number(row.soldTicketCount),
    totalRevenue: (row) => Number(row.totalRevenue),
    averageTicketPrice: (row) => Number(row.averageTicketPrice),
    paidOrderCount: (row) => Number(row.paidOrderCount),
    customerCount: (row) => Number(row.customerCount),
  });
  const handleSort = (field) => setSort((current) => nextSortState(current, field));

  return (
    <AnalyticsTable title="Summary KPIs">
      <TableHead>
        <TableRow>
          <SortableTableCell sort={sort} field="currencyCode" onSort={handleSort}>Currency</SortableTableCell>
          <SortableTableCell sort={sort} field="soldTicketCount" onSort={handleSort} align="right">Tickets</SortableTableCell>
          <SortableTableCell sort={sort} field="totalRevenue" onSort={handleSort} align="right">Revenue</SortableTableCell>
          <SortableTableCell sort={sort} field="averageTicketPrice" onSort={handleSort} align="right">Avg ticket</SortableTableCell>
          <SortableTableCell sort={sort} field="paidOrderCount" onSort={handleSort} align="right">Orders</SortableTableCell>
          <SortableTableCell sort={sort} field="customerCount" onSort={handleSort} align="right">Customers</SortableTableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {sortedRows.length ? sortedRows.map((row) => (
          <TableRow key={row.currencyCode} hover>
            <TableCell>{row.currencyCode}</TableCell>
            <TableCell align="right">{formatNumber(row.soldTicketCount)}</TableCell>
            <TableCell align="right">{formatMoney(row.totalRevenue, row.currencyCode)}</TableCell>
            <TableCell align="right">{formatMoney(row.averageTicketPrice, row.currencyCode)}</TableCell>
            <TableCell align="right">{formatNumber(row.paidOrderCount)}</TableCell>
            <TableCell align="right">{formatNumber(row.customerCount)}</TableCell>
          </TableRow>
        )) : <EmptyRows colSpan={6} />}
      </TableBody>
    </AnalyticsTable>
  );
}

function RevenueByRouteTable({ rows }) {
  const [sort, setSort] = useState({ field: "routeNumber", direction: "asc" });
  const sortedRows = sortRows(rows, sort, {
    soldTicketCount: (row) => Number(row.soldTicketCount),
    totalRevenue: (row) => Number(row.totalRevenue),
    averageTicketPrice: (row) => Number(row.averageTicketPrice),
    paidOrderCount: (row) => Number(row.paidOrderCount),
    customerCount: (row) => Number(row.customerCount),
  });
  const handleSort = (field) => setSort((current) => nextSortState(current, field));

  return (
    <AnalyticsTable title="Revenue by route">
      <TableHead>
        <TableRow>
          <SortableTableCell sort={sort} field="routeNumber" onSort={handleSort}>Route</SortableTableCell>
          <SortableTableCell sort={sort} field="soldTicketCount" onSort={handleSort} align="right">Tickets</SortableTableCell>
          <SortableTableCell sort={sort} field="totalRevenue" onSort={handleSort} align="right">Revenue</SortableTableCell>
          <SortableTableCell sort={sort} field="averageTicketPrice" onSort={handleSort} align="right">Avg ticket</SortableTableCell>
          <SortableTableCell sort={sort} field="paidOrderCount" onSort={handleSort} align="right">Orders</SortableTableCell>
          <SortableTableCell sort={sort} field="customerCount" onSort={handleSort} align="right">Customers</SortableTableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {sortedRows.length ? sortedRows.map((row) => (
          <TableRow key={`${row.routeId}-${row.currencyCode}`} hover>
            <TableCell>{row.routeNumber} - {row.routeName}</TableCell>
            <TableCell align="right">{formatNumber(row.soldTicketCount)}</TableCell>
            <TableCell align="right">{formatMoney(row.totalRevenue, row.currencyCode)}</TableCell>
            <TableCell align="right">{formatMoney(row.averageTicketPrice, row.currencyCode)}</TableCell>
            <TableCell align="right">{formatNumber(row.paidOrderCount)}</TableCell>
            <TableCell align="right">{formatNumber(row.customerCount)}</TableCell>
          </TableRow>
        )) : <EmptyRows colSpan={6} />}
      </TableBody>
    </AnalyticsTable>
  );
}

function PopularSegmentsTable({ rows }) {
  const [sort, setSort] = useState({ field: "startStationName", direction: "asc" });
  const sortedRows = sortRows(rows, sort, {
    ticketCount: (row) => Number(row.ticketCount),
    totalRevenue: (row) => Number(row.totalRevenue),
  });
  const handleSort = (field) => setSort((current) => nextSortState(current, field));

  return (
    <AnalyticsTable title="Popular segments across all routes">
      <TableHead>
        <TableRow>
          <SortableTableCell sort={sort} field="startStationName" onSort={handleSort}>Segment</SortableTableCell>
          <SortableTableCell sort={sort} field="ticketCount" onSort={handleSort} align="right">Tickets</SortableTableCell>
          <SortableTableCell sort={sort} field="totalRevenue" onSort={handleSort} align="right">Revenue</SortableTableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {sortedRows.length ? sortedRows.map((row) => (
          <TableRow
            key={`${row.startStationName}-${row.endStationName}-${row.currencyCode}`}
            hover
          >
            <TableCell>{row.startStationName} - {row.endStationName}</TableCell>
            <TableCell align="right">{formatNumber(row.ticketCount)}</TableCell>
            <TableCell align="right">{formatMoney(row.totalRevenue, row.currencyCode)}</TableCell>
          </TableRow>
        )) : <EmptyRows colSpan={3} />}
      </TableBody>
    </AnalyticsTable>
  );
}

function SeatOccupancyTable({ title, rows }) {
  const [sort, setSort] = useState({ field: "departureAtUtc", direction: "asc" });
  const sortedRows = sortRows(rows, sort, {
    totalSeatCount: (row) => Number(row.totalSeatCount),
    occupancyPercent: (row) => Number(row.occupancyPercent),
  });
  const handleSort = (field) => setSort((current) => nextSortState(current, field));

  return (
    <AnalyticsTable title={title}>
      <TableHead>
        <TableRow>
          <SortableTableCell sort={sort} field="departureAtUtc" onSort={handleSort}>Departure</SortableTableCell>
          <SortableTableCell sort={sort} field="routeNumber" onSort={handleSort}>Route</SortableTableCell>
          <SortableTableCell sort={sort} field="totalSeatCount" onSort={handleSort} align="right">Seats</SortableTableCell>
          <SortableTableCell sort={sort} field="occupancyPercent" onSort={handleSort} align="right">Occupancy</SortableTableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {sortedRows.length ? sortedRows.map((row) => (
          <TableRow key={row.tripId} hover>
            <TableCell>{formatDateTime(row.departureAtUtc)}</TableCell>
            <TableCell>{row.routeNumber} - {row.routeName}</TableCell>
            <TableCell align="right">{formatNumber(row.totalSeatCount)}</TableCell>
            <TableCell align="right">{formatNumber(row.occupancyPercent, { maximumFractionDigits: 2 })}%</TableCell>
          </TableRow>
        )) : <EmptyRows colSpan={4} />}
      </TableBody>
    </AnalyticsTable>
  );
}

function AnalyticsChatPanel({ messages, value, onChange, onSubmit, isLoading, error }) {
  return (
    <Card>
      <CardContent sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Typography variant="h6">AI assistant</Typography>

          <Paper
            variant="outlined"
            sx={{
              p: 2,
              minHeight: 280,
              maxHeight: 420,
              overflowY: "auto",
              bgcolor: "rgba(255,255,255,0.72)",
            }}
          >
            <Stack spacing={1.5}>
              {messages.map((message) => (
                <Box
                  key={message.id}
                  sx={{
                    display: "flex",
                    justifyContent: message.role === "user" ? "flex-end" : "flex-start",
                  }}
                >
                  <Box
                    sx={{
                      maxWidth: { xs: "100%", md: "78%" },
                      p: 1.5,
                      borderRadius: 2,
                      bgcolor: message.role === "user" ? "primary.main" : "background.paper",
                      color: message.role === "user" ? "common.white" : "text.primary",
                      border: message.role === "user" ? "none" : "1px solid rgba(79,138,91,0.18)",
                      whiteSpace: "pre-wrap",
                    }}
                  >
                    <Typography variant="body2">{message.content}</Typography>
                  </Box>
                </Box>
              ))}

              {isLoading ? (
                <Box sx={{ display: "flex", justifyContent: "flex-start" }}>
                  <Box
                    sx={{
                      maxWidth: { xs: "100%", md: "78%" },
                      p: 1.5,
                      borderRadius: 2,
                      bgcolor: "background.paper",
                      border: "1px solid rgba(79,138,91,0.18)",
                    }}
                  >
                    <Stack direction="row" spacing={1.25} alignItems="center">
                      <CircularProgress size={18} />
                      <Typography variant="body2">Generating answer...</Typography>
                    </Stack>
                  </Box>
                </Box>
              ) : null}

              {!messages.length ? (
                <Typography variant="body2" color="text.secondary" align="center" sx={{ py: 8 }}>
                  No messages yet.
                </Typography>
              ) : null}
            </Stack>
          </Paper>

          {error ? <Alert severity="error">{error}</Alert> : null}

          <Stack component="form" onSubmit={onSubmit} direction={{ xs: "column", md: "row" }} spacing={1.5}>
            <TextField
              label="Message"
              value={value}
              onChange={(event) => onChange(event.target.value)}
              disabled={isLoading}
              multiline
              minRows={2}
              fullWidth
            />
            <Button
              type="submit"
              variant="contained"
              endIcon={<SendIcon />}
              disabled={isLoading || !value.trim()}
              sx={{ minWidth: 140, alignSelf: { md: "stretch" } }}
            >
              Send
            </Button>
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  );
}

export function AdminAnalyticsPage() {
  const { authorizedGet, authorizedPost, user } = useAuth();
  const [filters, setFilters] = useState(() => buildInitialFilters());
  const [routes, setRoutes] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [chatMessages, setChatMessages] = useState([]);
  const [chatMessage, setChatMessage] = useState("");
  const [conversationId, setConversationId] = useState(() => createConversationId());
  const [error, setError] = useState("");
  const [chatError, setChatError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isChatLoading, setIsChatLoading] = useState(false);
  const [isRoutesLoading, setIsRoutesLoading] = useState(true);

  async function loadRoutes() {
    setIsRoutesLoading(true);

    try {
      const data = await authorizedGet("/api/admin/routes?page=0&size=1000&sort=number,asc");
      setRoutes(data.content ?? []);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setIsRoutesLoading(false);
    }
  }

  async function loadAnalytics(nextFilters = filters) {
    setIsLoading(true);
    setError("");

    try {
      const params = new URLSearchParams({
        dateFrom: toStartOfLocalDayIso(nextFilters.dateFrom),
        dateTo: toEndOfLocalDayExclusiveIso(nextFilters.dateTo),
        limit: String(nextFilters.limit),
      });

      if (nextFilters.routeId) {
        params.set("routeId", nextFilters.routeId);
      }

      const data = await authorizedGet(`/api/admin/analytics?${params.toString()}`);
      setAnalytics(data);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadRoutes();
    loadAnalytics(filters);
  }, []);

  function updateFilter(field, value) {
    setFilters((current) => ({ ...current, [field]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    loadAnalytics(filters);
  }

  async function handleChatSubmit(event) {
    event.preventDefault();

    const normalizedMessage = chatMessage.trim();
    if (!normalizedMessage) {
      return;
    }

    const userMessage = {
      id: `user-${Date.now()}`,
      role: "user",
      content: normalizedMessage,
    };

    setChatMessages((current) => [...current, userMessage]);
    setChatMessage("");
    setChatError("");
    setIsChatLoading(true);

    try {
      const response = await authorizedPost("/api/admin/analytics/chat", {
        conversationId,
        dateFrom: toStartOfLocalDayIso(filters.dateFrom),
        dateTo: toEndOfLocalDayExclusiveIso(filters.dateTo),
        routeId: filters.routeId ? Number(filters.routeId) : null,
        limit: Number(filters.limit),
        message: normalizedMessage,
      });

      if (response.conversationId) {
        setConversationId(response.conversationId);
      }

      setChatMessages((current) => [
        ...current,
        {
          id: `assistant-${Date.now()}`,
          role: "assistant",
          content: response.answer,
        },
      ]);
    } catch (submitError) {
      setChatError(submitError.message);
    } finally {
      setIsChatLoading(false);
    }
  }

  return (
    <Box sx={{ py: 5 }}>
      <Container maxWidth="xl">
        <Stack spacing={3}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Stack direction={{ xs: "column", md: "row" }} spacing={2} alignItems={{ md: "center" }}>
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
                  <AnalyticsIcon />
                </Box>
                <Box sx={{ flexGrow: 1 }}>
                  <Typography variant="overline" color="primary.main" sx={{ letterSpacing: "0.16em", fontWeight: 700 }}>
                    Admin panel
                  </Typography>
                  <Typography variant="h4">Analytics</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Logged in as {user?.firstName} {user?.lastName} ({user?.email})
                  </Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>

          <Card>
            <CardContent sx={{ p: 3 }}>
              <Stack component="form" onSubmit={handleSubmit} spacing={2}>
                <Stack direction={{ xs: "column", md: "row" }} spacing={2} alignItems={{ md: "flex-end" }}>
                  <TextField
                    label="Start date"
                    type="date"
                    value={filters.dateFrom}
                    onChange={(event) => updateFilter("dateFrom", event.target.value)}
                    slotProps={{ inputLabel: { shrink: true } }}
                    required
                    fullWidth
                  />
                  <TextField
                    label="End date"
                    type="date"
                    value={filters.dateTo}
                    onChange={(event) => updateFilter("dateTo", event.target.value)}
                    slotProps={{ inputLabel: { shrink: true } }}
                    required
                    fullWidth
                  />
                  <FormControl fullWidth disabled={isRoutesLoading}>
                    <InputLabel id="analytics-route-label">Route</InputLabel>
                    <Select
                      labelId="analytics-route-label"
                      label="Route"
                      value={filters.routeId}
                      onChange={(event) => updateFilter("routeId", event.target.value)}
                    >
                      <MenuItem value="">All routes</MenuItem>
                      {routes.map((route) => (
                        <MenuItem key={route.id} value={String(route.id)}>
                          {route.number} - {route.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <TextField
                    label="Top rows"
                    type="number"
                    value={filters.limit}
                    onChange={(event) => updateFilter("limit", event.target.value)}
                    slotProps={{ htmlInput: { min: 1, max: 100 } }}
                    sx={{ minWidth: { md: 120 } }}
                    required
                  />
                  <Button
                    type="submit"
                    variant="contained"
                    startIcon={<RefreshIcon />}
                    disabled={isLoading}
                    sx={{ minWidth: 160 }}
                  >
                    Build
                  </Button>
                </Stack>

                {error ? <Alert severity="error">{error}</Alert> : null}
              </Stack>
            </CardContent>
          </Card>

          {isLoading ? (
            <Card>
              <CardContent sx={{ py: 8, display: "grid", placeItems: "center" }}>
                <Stack spacing={2} alignItems="center">
                  <CircularProgress />
                  <Typography color="text.secondary">Loading analytics...</Typography>
                </Stack>
              </CardContent>
            </Card>
          ) : null}

          {analytics ? (
            <Stack spacing={3}>
              <SummaryKpiTable rows={analytics.summary ?? []} />
              <RevenueByRouteTable rows={analytics.revenueByRoute ?? []} />
              <PopularSegmentsTable rows={analytics.popularSegments ?? []} />
              <Box
                sx={{
                  display: "grid",
                  gap: 3,
                  gridTemplateColumns: { xs: "1fr", xl: "1fr 1fr" },
                }}
              >
                <SeatOccupancyTable title="High seat occupancy" rows={analytics.highSeatOccupancy ?? []} />
                <SeatOccupancyTable title="Low seat occupancy" rows={analytics.lowSeatOccupancy ?? []} />
              </Box>
            </Stack>
          ) : null}

          <AnalyticsChatPanel
            messages={chatMessages}
            value={chatMessage}
            onChange={setChatMessage}
            onSubmit={handleChatSubmit}
            isLoading={isChatLoading}
            error={chatError}
          />
        </Stack>
      </Container>
    </Box>
  );
}
