// Soul Buddy — AI 테스트용 클라이언트.
// production endpoints (/api/sessions, /api/chat, /api/sessions/{id}/end, ...) 호출.
// JWT 는 /api/dev/login (local 프로파일 전용) 으로 발급 후 localStorage 에 보관.

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_KEY = "soulbuddy.devToken";
const USER_KEY = "soulbuddy.devUserId";
const DEFAULT_USER_ID = Number(process.env.NEXT_PUBLIC_DEV_USER_ID ?? 1);

export type PersonaType = "FRIEND" | "COUNSELOR";
export type EmotionTag =
  | "HAPPY"
  | "SAD"
  | "ANGRY"
  | "ANXIOUS"
  | "HURT"
  | "EMBARRASSED";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type Sender = "USER" | "ASSISTANT" | "SYSTEM";
export type SessionStatus = "ONGOING" | "ENDED" | "ABANDONED";
export type SummaryStatus = "PENDING" | "CREATED" | "FAILED";

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: { code: string; message: string } | null;
}

export interface DevLoginResponse {
  accessToken: string;
  userId: number;
}

export interface SessionCreateResponse {
  sessionId: string;
  personaType: PersonaType;
  openingMessage: string;
  recentSummary: string | null;
  createdAt: string;
}

export interface SessionEndResponse {
  sessionId: string;
  status: SessionStatus;
  summaryStatus: SummaryStatus;
  summaryText: string;
  situationText: string;
  emotionText: string;
  thoughtText: string;
  dominantEmotion: EmotionTag | null;
  emotionChange: string | null;
  memoryHint: string | null;
  endedAt: string;
}

export interface ChatResponse {
  assistantMessage: string;
  emotionTag: EmotionTag | null;
  riskLevel: RiskLevel | null;
  interventionType: string | null;
  ragUsed: boolean;
  aiModel: string | null;
  forcedSafety: boolean;
  summary: string | null;
  memoryHint: string | null;
  recommendedAction: string | null;
  showSafetyChoice: boolean;
  counselingCenterPath: string | null;
}

export interface SessionItem {
  sessionId: string;
  personaType: PersonaType;
  characterName: string | null;
  status: SessionStatus;
  preChatEmotion: EmotionTag | null;
  summaryStatus: SummaryStatus | null;
  quoteText: string | null;
  dominantEmotion: EmotionTag | null;
  emotionChange: string | null;
  startedAt: string;
  endedAt: string | null;
}

export interface SessionListResponse {
  sessions: SessionItem[];
  totalCount: number;
  page: number;
  size: number;
}

export interface ChatHistoryMessage {
  messageId: number;
  sender: Sender;
  content: string;
  emotionTag: EmotionTag | null;
  riskLevel: RiskLevel | null;
  interventionType: string | null;
  ragUsed: boolean;
  aiModel: string | null;
  createdAt: string;
}

export interface ChatHistoryResponse {
  sessionId: string;
  messages: ChatHistoryMessage[];
  totalCount: number;
  page: number;
  size: number;
}

export interface PreChatEmotionResponse {
  sessionId: string;
  preChatEmotion: EmotionTag;
}

// ───────────────────────── token 보관 ─────────────────────────

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function getUserId(): number {
  if (typeof window === "undefined") return DEFAULT_USER_ID;
  const v = window.localStorage.getItem(USER_KEY);
  return v ? Number(v) : DEFAULT_USER_ID;
}

export function setSession(accessToken: string, userId: number) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(TOKEN_KEY, accessToken);
  window.localStorage.setItem(USER_KEY, String(userId));
}

export function clearSession() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(USER_KEY);
}

// ───────────────────────── HTTP 코어 ─────────────────────────

async function request<T>(
  path: string,
  init: RequestInit = {},
  options: { auth?: boolean } = { auth: true }
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");

  if (options.auth !== false) {
    const token = getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const res = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });
  const text = await res.text();
  const json = text ? (JSON.parse(text) as ApiResponse<T>) : null;

  if (!res.ok) {
    const message = json?.error?.message ?? `HTTP ${res.status}`;
    const code = json?.error?.code ?? String(res.status);
    throw new Error(`[${code}] ${message}`);
  }
  if (json && json.success === false) {
    throw new Error(`[${json.error?.code}] ${json.error?.message}`);
  }
  return (json?.data ?? null) as T;
}

// ───────────────────────── API 메서드 ─────────────────────────

export const api = {
  async devLogin(userId: number = DEFAULT_USER_ID): Promise<DevLoginResponse> {
    const data = await request<DevLoginResponse>(
      "/api/dev/login",
      { method: "POST", body: JSON.stringify({ userId }) },
      { auth: false }
    );
    setSession(data.accessToken, data.userId);
    return data;
  },

  async ensureLogin(): Promise<number> {
    if (!getToken()) {
      await api.devLogin(DEFAULT_USER_ID);
    }
    return getUserId();
  },

  // 세션
  createSession(personaType: PersonaType) {
    return request<SessionCreateResponse>("/api/sessions", {
      method: "POST",
      body: JSON.stringify({ personaType }),
    });
  },

  endSession(sessionId: string) {
    return request<SessionEndResponse>(
      `/api/sessions/${encodeURIComponent(sessionId)}/end`,
      { method: "PATCH" }
    );
  },

  setPreChatEmotion(sessionId: string, preChatEmotion: EmotionTag) {
    return request<PreChatEmotionResponse>(
      `/api/sessions/${encodeURIComponent(sessionId)}/pre-chat-emotion`,
      { method: "PATCH", body: JSON.stringify({ preChatEmotion }) }
    );
  },

  listSessions(status?: SessionStatus, page = 0, size = 10) {
    const qs = new URLSearchParams();
    if (status) qs.set("status", status);
    qs.set("page", String(page));
    qs.set("size", String(size));
    return request<SessionListResponse>(`/api/sessions?${qs.toString()}`);
  },

  // 채팅
  chat(payload: {
    sessionId: string;
    personaType: PersonaType;
    message: string;
    recentSummary?: string | null;
  }) {
    return request<ChatResponse>("/api/chat", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  history(sessionId: string, page = 0, size = 100) {
    return request<ChatHistoryResponse>(
      `/api/chat/history/${encodeURIComponent(sessionId)}?page=${page}&size=${size}`
    );
  },
};

// ───────────────────────── 공용 상수 ─────────────────────────

export const EMOTION_LIST: EmotionTag[] = [
  "HAPPY",
  "SAD",
  "ANGRY",
  "ANXIOUS",
  "HURT",
  "EMBARRASSED",
];

export const EMOTION_LABEL: Record<EmotionTag, string> = {
  HAPPY: "기쁨",
  SAD: "슬픔",
  ANGRY: "화남",
  ANXIOUS: "불안",
  HURT: "상처",
  EMBARRASSED: "당황",
};

// PR-6: 사용자에게 RAG 트리거 키워드 힌트 노출용 (application.yml 와 동일)
export const RAG_TRIGGER_KEYWORDS = [
  "기억나",
  "지난번",
  "예전에",
  "말했던",
  "얘기했던",
  "저번에",
  "그때",
  "예전",
  "지난",
];
