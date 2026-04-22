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
    Paper,
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
    typeCode: "",
    number: "",
    name: "",
};

export function AdminTrainsPage() {
    const { authorizedGet, authorizedPost, authorizedDelete, user } = useAuth();
    const [trainsPage, setTrainsPage] = useState(null);
    const [form, setForm] = useState(emptyForm);
    const [feedback, setFeedback] = useState("");
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [pageIndex, setPageIndex] = useState(0);

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

    useEffect(() => {
        loadTrains(0);
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
            setFeedback("Train created.");
            setForm(emptyForm);
            await loadTrains(0);
        } catch (submitError) {
            setError(submitError.message);
        } finally {
            setIsSubmitting(false);
        }
    }

    async function deleteTrain(id) {
        if (!window.confirm("Are you sure you want to delete this train?")) {
            return;
        }

        setFeedback("");
        setError("");

        try {
            await authorizedDelete(`/api/admin/trains/${id}`);
            setFeedback("Train deleted.");

            const isLastItemOnPage = trainsPage?.content?.length === 1;
            const nextPageIndex = (isLastItemOnPage && pageIndex > 0) ? pageIndex - 1 : pageIndex;

            await loadTrains(nextPageIndex);
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
                                        <Typography variant="h4">Trains management</Typography>
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
                                        <Typography variant="h5">Create train</Typography>
                                        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                                            Adds a new train through the admin API.
                                        </Typography>
                                    </Box>

                                    <TextField
                                        label="Type code"
                                        value={form.typeCode}
                                        onChange={(event) => setForm({ ...form, typeCode: event.target.value })}
                                        slotProps={{
                                            htmlInput: { maxLength: 30 },
                                        }}
                                        required
                                        fullWidth
                                    />

                                    <TextField
                                        label="Number"
                                        value={form.number}
                                        onChange={(event) => setForm({ ...form, number: event.target.value })}
                                        slotProps={{
                                            htmlInput: { maxLength: 50 },
                                        }}
                                        required
                                        fullWidth
                                    />

                                    <TextField
                                        label="Name"
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
                                        {isSubmitting ? "Saving..." : "Create train"}
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
                                            <Typography variant="h5">Existing trains</Typography>
                                        </Box>
                                        <Button
                                            variant="outlined"
                                            startIcon={<RefreshIcon />}
                                            onClick={() => loadTrains(pageIndex)}
                                        >
                                            Refresh
                                        </Button>
                                    </Stack>

                                    {feedback ? <Alert severity="success" onClose={() => setFeedback("")}>{feedback}</Alert> : null}

                                    {isLoading ? (
                                        <Box sx={{ py: 8, display: "grid", placeItems: "center" }}>
                                            <Stack spacing={2} alignItems="center">
                                                <CircularProgress />
                                                <Typography color="text.secondary">Loading trains...</Typography>
                                            </Stack>
                                        </Box>
                                    ) : null}

                                    {!isLoading && trainsPage?.content?.length ? (
                                        <>
                                            <TableContainer component={Paper} variant="outlined">
                                                <Table>
                                                    <TableHead>
                                                        <TableRow>
                                                            <TableCell>Type</TableCell>
                                                            <TableCell>Number</TableCell>
                                                            <TableCell>Name</TableCell>
                                                            <TableCell>Action</TableCell>
                                                        </TableRow>
                                                    </TableHead>
                                                    <TableBody>
                                                        {trainsPage.content.map((train) => (
                                                            <TableRow key={train.id} hover>
                                                                <TableCell>{train.typeCode}</TableCell>
                                                                <TableCell>{train.number}</TableCell>
                                                                <TableCell>{train.name}</TableCell>
                                                                <TableCell>
                                                                    <Button
                                                                        variant="contained"
                                                                        color="error"
                                                                        onClick={() => {deleteTrain(train.id)}}
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
                                                    onClick={() => loadTrains(pageIndex - 1)}
                                                    disabled={trainsPage.first}
                                                >
                                                    Previous
                                                </Button>
                                                <Typography variant="body2" color="text.secondary">
                                                    Page {trainsPage.number + 1} of {trainsPage.totalPages || 1}
                                                </Typography>
                                                <Button
                                                    variant="outlined"
                                                    onClick={() => loadTrains(pageIndex + 1)}
                                                    disabled={trainsPage.last}
                                                >
                                                    Next
                                                </Button>
                                            </Stack>
                                        </>
                                    ) : null}

                                    {!isLoading && !trainsPage?.content?.length ? (
                                        <Alert severity="info">No trains found yet.</Alert>
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
