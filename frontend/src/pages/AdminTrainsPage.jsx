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
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { nextSortState, SortableTableCell, sortRows } from "../components/SortableTableCell.jsx";
import { useI18n } from "../i18n/I18nContext.jsx";

const emptyForm = {
    typeCode: "",
    number: "",
    name: "",
};

export function AdminTrainsPage() {
    const { authorizedGet, authorizedPost, authorizedDelete, user } = useAuth();
    const { codeLabel, t } = useI18n();
    const [trainsPage, setTrainsPage] = useState(null);
    const [trainTypes, setTrainTypes] = useState([]);
    const [form, setForm] = useState(emptyForm);
    const [feedback, setFeedback] = useState("");
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [pageIndex, setPageIndex] = useState(0);
    const [sort, setSort] = useState({ field: "number", direction: "asc" });
    const sortedTrains = useMemo(
        () => sortRows(trainsPage?.content ?? [], sort),
        [trainsPage, sort],
    );

    async function loadTrains(page = pageIndex) {
        setIsLoading(true);
        setError("");

        try {
            const data = await authorizedGet(`/api/admin/trains?page=${page}&size=8&sort=number,asc`);
            setTrainsPage(data);
            setPageIndex(page);
        } catch (loadError) {
            setError(loadError.message);
        } finally {
            setIsLoading(false);
        }
    }

    async function loadTrainTypes() {
        try {
            const data = await authorizedGet("/api/admin/trains/types");
            setTrainTypes(data);
        } catch (loadError) {
            setError(loadError.message);
        }
    }

    useEffect(() => {
        loadTrains(0);
        loadTrainTypes();
    }, []);

    async function handleSubmit(event) {
        event.preventDefault();
        setFeedback("");
        setError("");
        setIsSubmitting(true);

        try {
            await authorizedPost("/api/admin/trains", {
                ...form,
            });
            setFeedback(t("Train created."));
            setForm(emptyForm);
            await loadTrains(0);
        } catch (submitError) {
            setError(submitError.message);
        } finally {
            setIsSubmitting(false);
        }
    }

    async function deleteTrain(id) {
        if (!globalThis.confirm(t("Are you sure you want to delete this train?"))) {
            return;
        }

        setFeedback("");
        setError("");

        try {
            await authorizedDelete(`/api/admin/trains/${id}`);
            setFeedback(t("Train deleted."));

            const isLastItemOnPage = trainsPage?.content?.length === 1;
            const nextPageIndex = (isLastItemOnPage && pageIndex > 0) ? pageIndex - 1 : pageIndex;

            await loadTrains(nextPageIndex);
        } catch (deleteError) {
            setError(deleteError.message);
        }
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
                                            {t("Admin panel")}
                                        </Typography>
                                        <Typography variant="h4">{t("Trains management")}</Typography>
                                        <Typography variant="body2" color="text.secondary">
                                            {t("Logged in as {name} ({email})", {
                                                name: [user?.firstName, user?.lastName].filter(Boolean).join(" "),
                                                email: user?.email,
                                            })}
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
                                        <Typography variant="h5">{t("Create train")}</Typography>
                                        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                                            {t("Register a train number, type, and display name.")}
                                        </Typography>
                                    </Box>

                                    <FormControl fullWidth required>
                                        <InputLabel id="train-type-label">{t("Type")}</InputLabel>
                                        <Select
                                            labelId="train-type-label"
                                            label={t("Type")}
                                            value={form.typeCode}
                                            onChange={(event) => setForm({ ...form, typeCode: event.target.value })}
                                        >
                                            {trainTypes.map((type) => (
                                                <MenuItem key={type.code} value={type.code}>
                                                    {codeLabel("trainType", type.code)} x{type.priceMultiplier}
                                                </MenuItem>
                                            ))}
                                        </Select>
                                    </FormControl>

                                    <TextField
                                        label={t("Number")}
                                        value={form.number}
                                        onChange={(event) => setForm({ ...form, number: event.target.value })}
                                        slotProps={{
                                            htmlInput: { maxLength: 50 },
                                        }}
                                        required
                                        fullWidth
                                    />

                                    <TextField
                                        label={t("Name")}
                                        value={form.name}
                                        onChange={(event) => setForm({ ...form, name: event.target.value })}
                                        slotProps={{
                                            htmlInput: { maxLength: 100 },
                                        }}
                                        required
                                        fullWidth
                                    />

                                    {error ? <Alert severity="error">{error}</Alert> : null}

                                    <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                                        {isSubmitting ? t("Saving...") : t("Create train")}
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
                                            <Typography variant="h5">{t("Existing trains")}</Typography>
                                        </Box>
                                        <Button
                                            variant="outlined"
                                            startIcon={<RefreshIcon />}
                                            onClick={() => loadTrains(pageIndex)}
                                        >
                                            {t("Refresh")}
                                        </Button>
                                    </Stack>

                                    {feedback ? <Alert severity="success" onClose={() => setFeedback("")}>{feedback}</Alert> : null}

                                    {isLoading ? (
                                        <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                                            <Stack spacing={2} alignItems="center">
                                                <CircularProgress />
                                                <Typography color="text.secondary">{t("Loading trains...")}</Typography>
                                            </Stack>
                                        </Box>
                                    ) : null}

                                    {!isLoading && trainsPage?.content?.length ? (
                                        <>
                                            <TableContainer component={Paper} variant="outlined">
                                                <Table>
                                                    <TableHead>
                                                        <TableRow>
                                                            <SortableTableCell sort={sort} field="number" onSort={handleSort}>{t("Number")}</SortableTableCell>
                                                            <SortableTableCell sort={sort} field="typeCode" onSort={handleSort}>{t("Type")}</SortableTableCell>
                                                            <SortableTableCell sort={sort} field="name" onSort={handleSort}>{t("Name")}</SortableTableCell>
                                                            <TableCell>{t("Action")}</TableCell>
                                                        </TableRow>
                                                    </TableHead>
                                                    <TableBody>
                                                        {sortedTrains.map((train) => (
                                                            <TableRow key={train.id} hover>
                                                                <TableCell>{train.number}</TableCell>
                                                                <TableCell>{codeLabel("trainType", train.typeCode)}</TableCell>
                                                                <TableCell>{train.name}</TableCell>
                                                                <TableCell>
                                                                    <Button
                                                                        variant="contained"
                                                                        color="error"
                                                                        onClick={() => {deleteTrain(train.id)}}
                                                                    >
                                                                        {t("Delete")}
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
                                                    onClick={() => loadTrains(pageIndex - 1)}
                                                    disabled={trainsPage.first}
                                                >
                                                    {t("Previous")}
                                                </Button>
                                                <Typography variant="body2" color="text.secondary">
                                                    {t("Page {current} of {total}", { current: trainsPage.number + 1, total: trainsPage.totalPages || 1 })}
                                                </Typography>
                                                <Button
                                                    variant="outlined"
                                                    onClick={() => loadTrains(pageIndex + 1)}
                                                    disabled={trainsPage.last}
                                                >
                                                    {t("Next")}
                                                </Button>
                                            </Stack>
                                        </>
                                    ) : null}

                                    {!isLoading && !trainsPage?.content?.length ? (
                                        <Alert severity="info">{t("No trains found yet.")}</Alert>
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
