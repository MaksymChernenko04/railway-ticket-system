import AssistantIcon from "@mui/icons-material/Assistant";
import SendIcon from "@mui/icons-material/Send";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { useAuth } from "../auth/AuthContext";

function createConversationId() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }

  return `client-chat-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

const emptyChatText = "Твій віртуальний асистент на основі ШІ";

function ChatMessage({ message }) {
  const isUser = message.role === "user";

  return (
    <Box sx={{ display: "flex", justifyContent: isUser ? "flex-end" : "flex-start" }}>
      <Box
        sx={{
          maxWidth: { xs: "100%", md: "76%" },
          p: 1.5,
          borderRadius: 2,
          bgcolor: isUser ? "primary.main" : "background.paper",
          color: isUser ? "common.white" : "text.primary",
          border: isUser ? "none" : "1px solid rgba(79,138,91,0.18)",
          whiteSpace: "pre-wrap",
        }}
      >
        <Typography variant="body2">{message.content}</Typography>
      </Box>
    </Box>
  );
}

export function TravelAssistantPage() {
  const { authorizedPost, user } = useAuth();
  const [conversationId, setConversationId] = useState(() => createConversationId());
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  function startNewChat() {
    setConversationId(createConversationId());
    setMessages([]);
    setMessage("");
    setError("");
  }

  async function sendMessage(text) {
    const normalizedMessage = text.trim();
    if (!normalizedMessage || isLoading) {
      return;
    }

    const userMessage = {
      id: `user-${Date.now()}`,
      role: "user",
      content: normalizedMessage,
    };

    setMessages((current) => [...current, userMessage]);
    setMessage("");
    setError("");
    setIsLoading(true);

    try {
      const response = await authorizedPost("/api/client/assistant/chat", {
        conversationId,
        message: normalizedMessage,
      });

      if (response?.conversationId && response.conversationId !== conversationId) {
        setConversationId(response.conversationId);
      }

      setMessages((current) => [
        ...current,
        {
          id: `assistant-${Date.now()}`,
          role: "assistant",
          content: response.answer,
        },
      ]);
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setIsLoading(false);
    }
  }

  function handleSubmit(event) {
    event.preventDefault();
    sendMessage(message);
  }

  return (
    <Box
      sx={{
        minHeight: "calc(100vh - 64px)",
        py: 5,
        background:
          "radial-gradient(circle at top left, rgba(79,138,91,0.20), transparent 28%), linear-gradient(135deg, #e8eef2 0%, #dde9e3 100%)",
      }}
    >
      <Container maxWidth="lg">
        <Stack spacing={3}>
          <Card sx={{ bgcolor: "rgba(255,255,255,0.86)", backdropFilter: "blur(12px)" }}>
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
                    <AssistantIcon />
                  </Box>
                  <Box>
                    <Typography
                      variant="overline"
                      color="primary.main"
                      sx={{ letterSpacing: "0.16em", fontWeight: 700 }}
                    >
                      Assistant
                    </Typography>
                    <Typography variant="h4">Travel assistant</Typography>
                  </Box>
                </Stack>
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                  <Chip color="primary" variant="outlined" label={user?.email ?? "Client"} />
                  <Button variant="outlined" onClick={startNewChat}>
                    New chat
                  </Button>
                </Stack>
              </Stack>
            </CardContent>
          </Card>

          <Card>
            <CardContent sx={{ p: { xs: 2, md: 3 } }}>
              <Stack spacing={2}>
                <Paper
                  variant="outlined"
                  sx={{
                    p: 2,
                    minHeight: { xs: 360, md: 480 },
                    maxHeight: { xs: 520, md: 620 },
                    overflowY: "auto",
                    bgcolor: "rgba(255,255,255,0.72)",
                  }}
                >
                  <Stack spacing={1.5}>
                    {messages.map((chatMessage) => (
                      <ChatMessage key={chatMessage.id} message={chatMessage} />
                    ))}

                    {isLoading ? (
                      <Box sx={{ display: "flex", justifyContent: "flex-start" }}>
                        <Box
                          sx={{
                            maxWidth: { xs: "100%", md: "76%" },
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
                      <Stack spacing={2} alignItems="center" sx={{ py: 8 }}>
                        <AssistantIcon color="primary" fontSize="large" />
                        <Typography color="text.secondary" textAlign="center">
                          {emptyChatText}
                        </Typography>
                      </Stack>
                    ) : null}
                  </Stack>
                </Paper>

                {error ? <Alert severity="error">{error}</Alert> : null}

                <Stack component="form" onSubmit={handleSubmit} direction={{ xs: "column", md: "row" }} spacing={1.5}>
                  <TextField
                    label="Message"
                    value={message}
                    onChange={(event) => setMessage(event.target.value)}
                    disabled={isLoading}
                    multiline
                    minRows={2}
                    fullWidth
                  />
                  <Button
                    type="submit"
                    variant="contained"
                    endIcon={<SendIcon />}
                    disabled={isLoading || !message.trim()}
                    sx={{ minWidth: 140, alignSelf: { md: "stretch" } }}
                  >
                    Send
                  </Button>
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        </Stack>
      </Container>
    </Box>
  );
}
