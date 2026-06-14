import RefreshIcon from "@mui/icons-material/Refresh";
import TrainIcon from "@mui/icons-material/Train";
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
import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";

const emptyForm = {
    number: "",
    name: "",
    basePrice: "",
    currencyCode: "",
};

export function AdminRoutesPage() {
    const { authorizedGet, authorizedPost, authorizedDelete, user } = useAuth();
    const [routesPage, setRoutesPage] = useState(null);
    const [currencies, setCurrencies] = useState([]);
    const [form, setForm] = useState(emptyForm);
    const [feedback, setFeedback] = useState("");
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [pageIndex, setPageIndex] = useState(0);

    async function loadRoutes(page = pageIndex) {
        setIsLoading(true);
        setError("");

        try {
            const data = await authorizedGet(`/api/admin/routes?page=${page}&size=8&sort=number,asc`);
            setRoutesPage(data);
            setPageIndex(page);
        } catch (loadError) {
            setError(loadError.message);
        } finally {
            setIsLoading(false);
        }
    }

    async function loadCurrencies() {
        try {
            const data = await authorizedGet("/api/admin/routes/currencies");
            setCurrencies(data);
        } catch (loadError) {
            setError(loadError.message);
        }
    }

    useEffect(() => {
        loadRoutes(0);
        loadCurrencies();
    }, []);

    async function handleSubmit(event) {
        event.preventDefault();
        setFeedback("");
        setError("");
        setIsSubmitting(true);

        try {
            await authorizedPost("/api/admin/routes", {
                ...form,
            });
            setFeedback("Route created.");
            setForm(emptyForm);
            await loadRoutes(0);
        } catch (submitError) {
            setError(submitError.message);
        } finally {
            setIsSubmitting(false);
        }
    }

    async function deleteRoute(id) {
        if (!globalThis.confirm("Are you sure you want to delete this route?")) {
            return;
        }

        setFeedback("");
        setError("");

        try {
            await authorizedDelete(`/api/admin/routes/${id}`);
            setFeedback("Route deleted.");

            const isLastItemOnPage = routesPage?.content?.length === 1;
            const nextPageIndex = (isLastItemOnPage && pageIndex > 0) ? pageIndex - 1 : pageIndex;

            await loadRoutes(nextPageIndex);
        } catch (deleteError) {
            setError(deleteError.message);
        }
    }

    return (
        <Box
            sx={{
                minHeight: "100vh",
                py: 5,
                background:
                    "radial-gradient(circle at top left, rgba(92,133,255,0.18), transparent 26%), linear-gradient(135deg, #f4f7fb 0%, #edf7f3 100%)",
            }}
        >
            <Container maxWidth="xl">
                <Stack spacing={3}>
                    <Card sx={{ bgcolor: "rgba(255,255,255,0.84)", backdropFilter: "blur(12px)" }}>
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
                                            Admin panel
                                        </Typography>
                                        <Typography variant="h4">Routes management</Typography>
                                        <Typography variant="body2" color="text.secondary">
                                            Logged in as {user?.firstName} {user?.lastName} ({user?.email})
                                        </Typography>
                                    </Box>
                                </Stack>
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
                                        <Typography variant="h5">Create route</Typography>
                                        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                                            Set the public route number, name, base price, and currency.
                                        </Typography>
                                    </Box>

                                    <TextField
                                        label="Number"
                                        value={form.number}
                                        onChange={(event) => setForm({ ...form, number: event.target.value })}
                                        slotProps={{
                                            htmlInput: { maxLength: 20 },
                                        }}
                                        required
                                        fullWidth
                                    />

                                    <TextField
                                        label="Name"
                                        value={form.name}
                                        onChange={(event) => setForm({ ...form, name: event.target.value })}
                                        required
                                        fullWidth
                                    />

                                    <TextField
                                        label="Base price"
                                        value={form.basePrice}
                                        onChange={
                                        (event) => {
                                            let value = event.target.value.replace(",", ".");
                                            if (value === ".") value = "0.";

                                            if (/^\d*\.?\d{0,2}$/.test(value)) {
                                                setForm({...form, basePrice: value})
                                            }
                                        }}
                                        inputMode="decimal"
                                        required
                                        fullWidth
                                    />

                                    <FormControl fullWidth required>
                                        <InputLabel id="route-currency-label">Currency</InputLabel>
                                        <Select
                                            labelId="route-currency-label"
                                            label="Currency"
                                            value={form.currencyCode}
                                            onChange={(event) => setForm({ ...form, currencyCode: event.target.value })}
                                        >
                                            {currencies.map((currency) => (
                                                <MenuItem key={currency.code} value={currency.code}>
                                                    {currency.code}
                                                </MenuItem>
                                            ))}
                                        </Select>
                                    </FormControl>

                                    {error ? <Alert severity="error">{error}</Alert> : null}

                                    <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                                        {isSubmitting ? "Saving..." : "Create route"}
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
                                            <Typography variant="h5">Existing routes</Typography>
                                        </Box>
                                        <Button
                                            variant="outlined"
                                            startIcon={<RefreshIcon />}
                                            onClick={() => loadRoutes(pageIndex)}
                                        >
                                            Refresh
                                        </Button>
                                    </Stack>

                                    {feedback ? <Alert severity="success" onClose={() => setFeedback("")}>{feedback}</Alert> : null}

                                    {isLoading ? (
                                        <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                                            <Stack spacing={2} alignItems="center">
                                                <CircularProgress />
                                                <Typography color="text.secondary">Loading routes...</Typography>
                                            </Stack>
                                        </Box>
                                    ) : null}

                                    {!isLoading && routesPage?.content?.length ? (
                                        <>
                                            <TableContainer component={Paper} variant="outlined">
                                                <Table>
                                                    <TableHead>
                                                        <TableRow>
                                                            <TableCell>Number</TableCell>
                                                            <TableCell>Name</TableCell>
                                                            <TableCell>Base price</TableCell>
                                                            <TableCell>Currency</TableCell>
                                                            <TableCell>Action</TableCell>
                                                        </TableRow>
                                                    </TableHead>
                                                    <TableBody>
                                                        {routesPage.content.map((route) => (
                                                            <TableRow key={route.id} hover>
                                                                <TableCell>{route.number}</TableCell>
                                                                <TableCell>{route.name}</TableCell>
                                                                <TableCell>{route.basePrice}</TableCell>
                                                                <TableCell>{route.currencyCode}</TableCell>
                                                                <TableCell>
                                                                    <Button
                                                                        variant="contained"
                                                                        color="error"
                                                                        onClick={() => {deleteRoute(route.id)}}
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
                                                    onClick={() => loadRoutes(pageIndex - 1)}
                                                    disabled={routesPage.first}
                                                >
                                                    Previous
                                                </Button>
                                                <Typography variant="body2" color="text.secondary">
                                                    Page {routesPage.number + 1} of {routesPage.totalPages || 1}
                                                </Typography>
                                                <Button
                                                    variant="outlined"
                                                    onClick={() => loadRoutes(pageIndex + 1)}
                                                    disabled={routesPage.last}
                                                >
                                                    Next
                                                </Button>
                                            </Stack>
                                        </>
                                    ) : null}

                                    {!isLoading && !routesPage?.content?.length ? (
                                        <Alert severity="info">No routes found yet.</Alert>
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
