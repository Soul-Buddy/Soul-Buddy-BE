"use client";

import {
  type FormEvent,
  type KeyboardEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  EMOTION_LABEL,
  EMOTION_LIST,
  RAG_TRIGGER_KEYWORDS,
  api,
  clearSession,
  getToken,
  getUserId,
  type ChatResponse,
  type EmotionTag,
  type PersonaType,
  type RiskLevel,
  type SessionEndResponse,
  type SessionItem,
} from "@/lib/api";

type Step = "lobby" | "persona" | "preEmotion" | "chat" | "summary";

interface ChatMsg {
  id: string;
  sender: "USER" | "ASSISTANT" | "SYSTEM";
  content: string;
  createdAt: string;
  meta?: {
    emotionTag?: EmotionTag | null;
    riskLevel?: RiskLevel | null;
    interventionType?: string | null;
    aiModel?: string | null;
    forcedSafety?: boolean;
    ragUsed?: boolean;
    showSafetyChoice?: boolean;
    counselingCenterPath?: string | null;
  };
}

interface PersonaOption {
  type: PersonaType;
  characterName: string;
  displayName: string;
  description: string;
  tags: string[];
  initial: string;
  toneSample: string;
}

const PERSONAS: PersonaOption[] = [
  {
    type: "FRIEND",
    characterName: "포코",
    displayName: "친구형",
    description: "반말·구어체로 일상에 가볍게 공감",
    tags: ["따뜻", "공감"],
    initial: "포",
    toneSample: "야 오늘 진짜 고생했어~ 무슨 일 있었어?",
  },
  {
    type: "COUNSELOR",
    characterName: "루미",
    displayName: "상담사형",
    description: "존댓말·정제된 어휘로 차분한 성찰",
    tags: ["차분", "성찰"],
    initial: "루",
    toneSample: "오늘 어떤 감정이 가장 크게 느껴지셨나요?",
  },
];

// PR-2 v2.3 — application.yml soulbuddy.in-session-summary.threshold (80)
const SLIDING_WINDOW_THRESHOLD = 80;
// PR-1 v2.3 — application.yml soulbuddy.safety.forced-safety-threshold (3)
const FORCED_SAFETY_THRESHOLD = 3;

function newMsgId() {
  return Math.random().toString(36).slice(2) + Date.now().toString(36);
}

function formatTime(iso: string) {
  const d = new Date(iso);
  const h = d.getHours();
  const m = d.getMinutes().toString().padStart(2, "0");
  const ap = h >= 12 ? "오후" : "오전";
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${ap} ${h12}:${m}`;
}

function formatDate(iso: string | null | undefined) {
  if (!iso) return "-";
  const d = new Date(iso);
  return `${d.getMonth() + 1}/${d.getDate()} ${formatTime(iso)}`;
}

export default function Page() {
  const [step, setStep] = useState<Step>("lobby");
  const [loggedIn, setLoggedIn] = useState(false);
  const [userId, setUserIdState] = useState<number>(1);

  const [persona, setPersona] = useState<PersonaOption | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [openingRecentSummary, setOpeningRecentSummary] = useState<
    string | null
  >(null);

  const [messages, setMessages] = useState<ChatMsg[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [summary, setSummary] = useState<SessionEndResponse | null>(null);

  // emotion 누적 카운터 (FE 측에서 매 응답 emotionTag 를 직접 누적 — UI 시각화용)
  const [emotionTally, setEmotionTally] = useState<Record<EmotionTag, number>>({
    HAPPY: 0,
    SAD: 0,
    ANGRY: 0,
    ANXIOUS: 0,
    HURT: 0,
    EMBARRASSED: 0,
  });
  const [highCount, setHighCount] = useState(0);
  const [ragUsedCount, setRagUsedCount] = useState(0);
  const [safetyChoiceOverlay, setSafetyChoiceOverlay] = useState<{
    path: string;
  } | null>(null);

  const [pastSessions, setPastSessions] = useState<SessionItem[]>([]);
  const [recentSummaryForChat, setRecentSummaryForChat] = useState<
    string | null
  >(null);

  const threadEndRef = useRef<HTMLDivElement | null>(null);
  const firstChatSentRef = useRef(false);

  // ─── 초기 로그인 ───
  // dev 모드 — 캐시된 토큰은 만료 가능성이 있으니 매 페이지 로드마다 새로 발급.
  useEffect(() => {
    (async () => {
      try {
        clearSession();
        const res = await api.devLogin();
        setLoggedIn(true);
        setUserIdState(res.userId);
      } catch (e) {
        report(e);
      }
    })();
  }, []);

  // ─── 자동 스크롤 ───
  useEffect(() => {
    threadEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages.length, busy]);

  // ─── 세션 목록 로드 ───
  const reloadSessions = useCallback(async () => {
    if (!loggedIn) return;
    try {
      const list = await api.listSessions(undefined, 0, 8);
      setPastSessions(list.sessions);
    } catch (e) {
      // 세션 0건이면 그대로
      console.warn("listSessions failed", e);
    }
  }, [loggedIn]);

  useEffect(() => {
    reloadSessions();
  }, [reloadSessions]);

  function report(e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    setError(msg);
    console.error(e);
  }

  function fullReset() {
    setStep("lobby");
    setPersona(null);
    setSessionId(null);
    setOpeningRecentSummary(null);
    setMessages([]);
    setInput("");
    setSummary(null);
    setEmotionTally({
      HAPPY: 0,
      SAD: 0,
      ANGRY: 0,
      ANXIOUS: 0,
      HURT: 0,
      EMBARRASSED: 0,
    });
    setHighCount(0);
    setRagUsedCount(0);
    setSafetyChoiceOverlay(null);
    setRecentSummaryForChat(null);
    firstChatSentRef.current = false;
    reloadSessions();
  }

  // ─── 페르소나 확정 → POST /api/sessions ───
  async function handlePickPersona(p: PersonaOption) {
    setBusy(true);
    setError(null);
    try {
      const created = await api.createSession(p.type);
      setPersona(p);
      setSessionId(created.sessionId);
      setOpeningRecentSummary(created.recentSummary ?? null);
      setRecentSummaryForChat(created.recentSummary ?? null);
      setMessages([
        {
          id: `opening-${created.sessionId}`,
          sender: "ASSISTANT",
          content: created.openingMessage,
          createdAt: created.createdAt,
          meta: { aiModel: `HCX-005-${p.type}` },
        },
      ]);
      firstChatSentRef.current = false;
      setStep("preEmotion");
    } catch (e) {
      report(e);
    } finally {
      setBusy(false);
    }
  }

  // ─── 사전 감정 선택 → PATCH /api/sessions/{id}/pre-chat-emotion ───
  async function handlePreEmotion(em: EmotionTag) {
    if (!sessionId) return;
    setBusy(true);
    setError(null);
    try {
      await api.setPreChatEmotion(sessionId, em);
      setStep("chat");
    } catch (e) {
      report(e);
    } finally {
      setBusy(false);
    }
  }

  // ─── 채팅 전송 → POST /api/chat ───
  async function handleSend(e?: FormEvent) {
    e?.preventDefault();
    if (!persona || !sessionId || !input.trim() || busy) return;
    const userText = input.trim();
    const now = new Date().toISOString();
    const userMsg: ChatMsg = {
      id: `user-${newMsgId()}`,
      sender: "USER",
      content: userText,
      createdAt: now,
    };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setBusy(true);
    setError(null);

    try {
      const res: ChatResponse = await api.chat({
        sessionId,
        personaType: persona.type,
        message: userText,
        // 첫 USER 메시지에만 recentSummary 전달, 이후 null
        recentSummary: firstChatSentRef.current ? null : recentSummaryForChat,
      });
      firstChatSentRef.current = true;

      if (res.emotionTag) {
        setEmotionTally((prev) => ({
          ...prev,
          [res.emotionTag as EmotionTag]:
            (prev[res.emotionTag as EmotionTag] ?? 0) + 1,
        }));
      }
      if (res.riskLevel === "HIGH") setHighCount((c) => c + 1);
      if (res.ragUsed) setRagUsedCount((c) => c + 1);

      setMessages((prev) => [
        ...prev,
        {
          id: `assistant-${newMsgId()}`,
          sender: res.forcedSafety ? "SYSTEM" : "ASSISTANT",
          content: res.assistantMessage,
          createdAt: new Date().toISOString(),
          meta: {
            emotionTag: res.emotionTag,
            riskLevel: res.riskLevel,
            interventionType: res.interventionType,
            aiModel: res.aiModel,
            forcedSafety: res.forcedSafety,
            ragUsed: res.ragUsed,
            showSafetyChoice: res.showSafetyChoice,
            counselingCenterPath: res.counselingCenterPath,
          },
        },
      ]);

      if (res.showSafetyChoice && res.counselingCenterPath) {
        setSafetyChoiceOverlay({ path: res.counselingCenterPath });
      }
    } catch (e) {
      report(e);
    } finally {
      setBusy(false);
    }
  }

  // ─── 끝내기 → PATCH /api/sessions/{id}/end ───
  async function handleEnd() {
    if (!sessionId || busy) return;
    if (typeof window !== "undefined" && !window.confirm("대화를 끝내고 HCX-007 요약을 받을까요?")) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const result = await api.endSession(sessionId);
      setSummary(result);
      setStep("summary");
      reloadSessions();
    } catch (e) {
      report(e);
    } finally {
      setBusy(false);
    }
  }

  // ─── derived ───
  const userMsgCount = useMemo(
    () => messages.filter((m) => m.sender === "USER").length,
    [messages]
  );
  const aiMsgCount = useMemo(
    () =>
      messages.filter((m) => m.sender === "ASSISTANT" || m.sender === "SYSTEM")
        .length,
    [messages]
  );
  const totalMsgCount = userMsgCount + aiMsgCount;

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-[1200px] flex-col gap-4 px-4 py-6 lg:flex-row">
      {/* ─── 좌측: Debug / 시나리오 패널 ─── */}
      <aside className="w-full lg:w-[320px] lg:shrink-0">
        <DebugPanel
          loggedIn={loggedIn}
          userId={userId}
          step={step}
          sessionId={sessionId}
          openingRecentSummary={openingRecentSummary}
          totalMsgCount={totalMsgCount}
          userMsgCount={userMsgCount}
          aiMsgCount={aiMsgCount}
          emotionTally={emotionTally}
          highCount={highCount}
          ragUsedCount={ragUsedCount}
          pastSessions={pastSessions}
          onLogout={() => {
            clearSession();
            window.location.reload();
          }}
          onReset={fullReset}
        />
      </aside>

      {/* ─── 메인: mobile-style 카드 ─── */}
      <section className="mx-auto flex w-full max-w-[480px] flex-col">
        <header className="mb-3 flex items-center justify-between rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] px-4 py-3 shadow-[var(--shadow-card)]">
          <div>
            <h1 className="text-sm font-semibold">Soul Buddy · AI 테스트</h1>
            <p className="text-[11px] text-[var(--color-text-muted)]">
              production endpoints · userId={userId}
            </p>
          </div>
          {step !== "lobby" && step !== "persona" && (
            <button
              type="button"
              onClick={fullReset}
              className="rounded-full border border-[var(--color-border)] bg-white/60 px-3 py-1 text-[11px] text-[var(--color-text-muted)] hover:bg-white"
            >
              처음으로
            </button>
          )}
        </header>

        {error && (
          <div className="mb-3 whitespace-pre-wrap rounded-xl border border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-3 py-2 text-xs text-[var(--color-danger)]">
            {error}
            <button
              type="button"
              className="ml-2 underline"
              onClick={() => setError(null)}
            >
              닫기
            </button>
          </div>
        )}

        {step === "lobby" && (
          <LobbyView
            loggedIn={loggedIn}
            userId={userId}
            pastSessions={pastSessions}
            onStart={() => setStep("persona")}
          />
        )}

        {step === "persona" && (
          <PersonaView busy={busy} onPick={handlePickPersona} />
        )}

        {step === "preEmotion" && persona && (
          <PreEmotionView
            persona={persona}
            openingRecentSummary={openingRecentSummary}
            openingMessage={messages[0]?.content ?? ""}
            busy={busy}
            onSkip={() => setStep("chat")}
            onPick={handlePreEmotion}
          />
        )}

        {step === "chat" && persona && (
          <ChatView
            persona={persona}
            messages={messages}
            input={input}
            setInput={setInput}
            busy={busy}
            highCount={highCount}
            totalMsgCount={totalMsgCount}
            onSend={handleSend}
            onEnd={handleEnd}
          />
        )}

        {step === "summary" && summary && (
          <SummaryView
            summary={summary}
            emotionTally={emotionTally}
            onRestart={fullReset}
          />
        )}
      </section>

      {/* ─── 안전 발화 오버레이 ─── */}
      {safetyChoiceOverlay && (
        <SafetyOverlay
          path={safetyChoiceOverlay.path}
          onClose={() => setSafetyChoiceOverlay(null)}
        />
      )}
    </main>
  );
}

// ────────────────────────────────────────────────────────────────────────────────
// Debug 패널
// ────────────────────────────────────────────────────────────────────────────────

function DebugPanel(props: {
  loggedIn: boolean;
  userId: number;
  step: Step;
  sessionId: string | null;
  openingRecentSummary: string | null;
  totalMsgCount: number;
  userMsgCount: number;
  aiMsgCount: number;
  emotionTally: Record<EmotionTag, number>;
  highCount: number;
  ragUsedCount: number;
  pastSessions: SessionItem[];
  onLogout: () => void;
  onReset: () => void;
}) {
  const {
    loggedIn,
    userId,
    step,
    sessionId,
    openingRecentSummary,
    totalMsgCount,
    userMsgCount,
    aiMsgCount,
    emotionTally,
    highCount,
    ragUsedCount,
    pastSessions,
    onLogout,
    onReset,
  } = props;
  const ratio = Math.min(1, totalMsgCount / SLIDING_WINDOW_THRESHOLD);
  const highRatio = Math.min(1, highCount / FORCED_SAFETY_THRESHOLD);

  return (
    <div className="flex flex-col gap-3">
      <PanelCard title="① 인증 / 세션">
        <Row label="logged in" value={loggedIn ? "✓" : "—"} />
        <Row label="userId" value={String(userId)} />
        <Row label="step" value={step} />
        <Row label="sessionId" value={sessionId ? sessionId.slice(0, 8) + "…" : "—"} mono />
        <button
          type="button"
          onClick={onLogout}
          className="mt-2 text-[11px] text-[var(--color-text-muted)] underline"
        >
          JWT 폐기 후 새로고침
        </button>
      </PanelCard>

      <PanelCard title="② recentSummary (자동 로드)">
        <p className="text-[11px] text-[var(--color-text-muted)]">
          POST /api/sessions 응답의 recentSummary — 직전 ENDED 세션 memoryHint
        </p>
        <div className="mt-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] p-2 text-[12px]">
          {openingRecentSummary ?? "(없음 — 첫 세션이거나 직전 세션 미요약)"}
        </div>
      </PanelCard>

      <PanelCard title="③ 슬라이딩 윈도우 (PR-2)">
        <Row label="USER 메시지" value={String(userMsgCount)} />
        <Row label="AI 메시지" value={String(aiMsgCount)} />
        <Row
          label="합산 / 임계값"
          value={`${totalMsgCount} / ${SLIDING_WINDOW_THRESHOLD}`}
        />
        <ProgressBar ratio={ratio} />
        <p className="mt-1 text-[10px] text-[var(--color-text-muted)]">
          {totalMsgCount >= SLIDING_WINDOW_THRESHOLD
            ? "임계값 도달 → @Async 압축 트리거 발생했을 가능성. BE 로그 확인."
            : "임계값 미달. 80 도달 시 가장 오래된 20개가 자동 요약(running_summary)으로 압축."}
        </p>
      </PanelCard>

      <PanelCard title="④ 감정 누적 (FE 시각화)">
        <p className="text-[11px] text-[var(--color-text-muted)]">
          매 응답의 emotionTag 를 누적. BE 는 emotion_logs 테이블에 동일 누적
          저장 → 세션 종료 시 HCX-007 요약에 [세션 메타].sessionEmotionCounts 로 주입.
        </p>
        <div className="mt-2 grid grid-cols-3 gap-1.5">
          {EMOTION_LIST.map((em) => (
            <div
              key={em}
              className="rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-center text-[11px]"
            >
              <div className="text-[10px] text-[var(--color-text-muted)]">
                {EMOTION_LABEL[em]}
              </div>
              <div className="font-semibold">{emotionTally[em]}</div>
            </div>
          ))}
        </div>
      </PanelCard>

      <PanelCard title="⑤ HIGH 누적 + Safety">
        <Row
          label="HIGH 카운트 / 임계값"
          value={`${highCount} / ${FORCED_SAFETY_THRESHOLD}`}
        />
        <ProgressBar ratio={highRatio} danger />
        <p className="mt-1 text-[10px] text-[var(--color-text-muted)]">
          BE: classifiedRisk=HIGH 이면서 누적 HIGH+1 ≥ 3 도달 시 강제 안전 발화
          + showSafetyChoice=true. (자기 위해 표현 예: &ldquo;죽고 싶다&rdquo;
          3번 누적)
        </p>
      </PanelCard>

      <PanelCard title="⑥ RAG (PR-6)">
        <Row label="ragUsed 누적" value={String(ragUsedCount)} />
        <p className="mt-1 text-[10px] text-[var(--color-text-muted)]">
          트리거 키워드 포함 시 검색. 결과 있을 때만 ragUsed=true.
          (자기 자신 세션 제외 + summary_id dedupe + Top-2)
        </p>
        <div className="mt-2 flex flex-wrap gap-1">
          {RAG_TRIGGER_KEYWORDS.map((k) => (
            <span
              key={k}
              className="rounded-full bg-[var(--color-surface-strong)] px-2 py-0.5 text-[10px] text-[var(--color-text-muted)]"
            >
              {k}
            </span>
          ))}
        </div>
      </PanelCard>

      <PanelCard title="과거 세션 (자동 로드 검증용)">
        <p className="text-[11px] text-[var(--color-text-muted)]">
          GET /api/sessions — 가장 최근 ENDED 세션의 memoryHint 가 다음 새 세션의
          recentSummary 로 자동 주입됨.
        </p>
        <div className="mt-2 flex max-h-[180px] flex-col gap-1 overflow-y-auto">
          {pastSessions.length === 0 && (
            <span className="text-[11px] text-[var(--color-text-muted)]">
              (세션 없음)
            </span>
          )}
          {pastSessions.map((s) => (
            <div
              key={s.sessionId}
              className="rounded-md border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1 text-[11px]"
            >
              <div className="flex items-center justify-between">
                <span className="font-medium">
                  {s.characterName ?? s.personaType}
                </span>
                <span
                  className={`rounded-full px-1.5 py-0 text-[9px] ${
                    s.status === "ENDED"
                      ? "bg-[var(--color-primary-soft)] text-[var(--color-primary-strong)]"
                      : "bg-[var(--color-surface-strong)] text-[var(--color-text-muted)]"
                  }`}
                >
                  {s.status}
                </span>
              </div>
              <div className="mt-0.5 text-[10px] text-[var(--color-text-muted)]">
                {formatDate(s.startedAt)}
                {s.dominantEmotion && ` · ${EMOTION_LABEL[s.dominantEmotion]}`}
              </div>
              {s.quoteText && (
                <div className="mt-1 line-clamp-2 text-[10px] text-[var(--color-text-muted)]">
                  &ldquo;{s.quoteText}&rdquo;
                </div>
              )}
            </div>
          ))}
        </div>
        <button
          type="button"
          onClick={onReset}
          className="mt-2 w-full rounded-md border border-[var(--color-border)] bg-white/60 py-1 text-[11px] text-[var(--color-text-muted)] hover:bg-white"
        >
          상태 초기화 (UI only)
        </button>
      </PanelCard>
    </div>
  );
}

function PanelCard({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-3 shadow-[var(--shadow-card)]">
      <h3 className="mb-2 text-[12px] font-semibold text-[var(--color-text)]">
        {title}
      </h3>
      {children}
    </div>
  );
}

function Row({
  label,
  value,
  mono,
}: {
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-0.5 text-[11px]">
      <span className="text-[var(--color-text-muted)]">{label}</span>
      <span className={mono ? "font-mono" : "font-medium"}>{value}</span>
    </div>
  );
}

function ProgressBar({
  ratio,
  danger = false,
}: {
  ratio: number;
  danger?: boolean;
}) {
  return (
    <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-[var(--color-border)]">
      <div
        className="h-full transition-all"
        style={{
          width: `${Math.max(2, ratio * 100)}%`,
          background: danger
            ? "var(--color-danger)"
            : "var(--color-primary)",
        }}
      />
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────────
// Lobby view
// ────────────────────────────────────────────────────────────────────────────────

function LobbyView(props: {
  loggedIn: boolean;
  userId: number;
  pastSessions: SessionItem[];
  onStart: () => void;
}) {
  const { loggedIn, userId, pastSessions, onStart } = props;
  const lastEnded = pastSessions.find((s) => s.status === "ENDED");

  return (
    <div className="flex flex-col gap-3">
      <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 text-center">
        <div className="mx-auto mb-3 flex h-16 w-16 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-2xl">
          ☁️
        </div>
        <h2 className="text-lg font-semibold">오늘 하루 어땠어요?</h2>
        <p className="mt-1 text-xs text-[var(--color-text-muted)]">
          AI 로직 통합 검증을 위한 dev UI 입니다.
        </p>
        <p className="mt-1 text-[11px] text-[var(--color-text-muted)]">
          {loggedIn ? `userId=${userId} 로 자동 로그인됨` : "로그인 중..."}
        </p>
        <button
          type="button"
          disabled={!loggedIn}
          onClick={onStart}
          className="mt-4 w-full rounded-full bg-[var(--color-primary)] py-3 text-sm font-semibold text-white hover:bg-[var(--color-primary-strong)] disabled:opacity-50"
        >
          새 대화 시작
        </button>
      </div>

      {lastEnded && (
        <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
          <h3 className="mb-1 text-sm font-semibold">최근 종료된 세션</h3>
          <p className="text-[11px] text-[var(--color-text-muted)]">
            다음 새 세션 시작 시 이 세션의 memoryHint 가 recentSummary 로 자동 주입됩니다.
          </p>
          <div className="mt-2 rounded-lg border border-[var(--color-border)] bg-white/60 p-2 text-xs">
            <div className="flex justify-between">
              <span className="font-medium">
                {lastEnded.characterName ?? lastEnded.personaType}
              </span>
              <span className="text-[var(--color-text-muted)]">
                {formatDate(lastEnded.endedAt)}
              </span>
            </div>
            {lastEnded.quoteText && (
              <p className="mt-1 text-[var(--color-text-muted)]">
                &ldquo;{lastEnded.quoteText}&rdquo;
              </p>
            )}
            {lastEnded.emotionChange && (
              <p className="mt-1 text-[10px] text-[var(--color-text-muted)]">
                감정 변화: {lastEnded.emotionChange}
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────────
// Persona view
// ────────────────────────────────────────────────────────────────────────────────

function PersonaView(props: {
  busy: boolean;
  onPick: (p: PersonaOption) => void;
}) {
  const { busy, onPick } = props;
  return (
    <div>
      <h2 className="mb-2 px-1 text-sm font-semibold">
        오늘은 누구와 이야기 할까요?
      </h2>
      <p className="mb-3 px-1 text-[11px] text-[var(--color-text-muted)]">
        선택과 동시에 POST /api/sessions 호출 → 오프닝 + recentSummary 수신.
      </p>
      <div className="flex flex-col gap-3">
        {PERSONAS.map((p) => (
          <button
            key={p.type}
            type="button"
            disabled={busy}
            onClick={() => onPick(p)}
            className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4 text-left transition hover:border-[var(--color-primary)] disabled:opacity-50"
          >
            <div className="mb-2 flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-base font-semibold text-[var(--color-primary-strong)]">
                {p.initial}
              </div>
              <div>
                <div className="text-base font-semibold">{p.characterName}</div>
                <div className="text-xs text-[var(--color-text-muted)]">
                  {p.displayName}
                </div>
              </div>
            </div>
            <p className="text-sm">{p.description}</p>
            <div className="mt-2 flex flex-wrap gap-1.5">
              {p.tags.map((t) => (
                <span
                  key={t}
                  className="rounded-full bg-[var(--color-primary-soft)] px-2 py-0.5 text-[10px] text-[var(--color-primary-strong)]"
                >
                  # {t}
                </span>
              ))}
            </div>
            <p className="mt-3 rounded-lg bg-white/60 px-3 py-2 text-xs italic text-[var(--color-text-muted)]">
              &ldquo;{p.toneSample}&rdquo;
            </p>
          </button>
        ))}
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────────
// Pre-chat emotion view (recentSummary 도 같이 노출)
// ────────────────────────────────────────────────────────────────────────────────

function PreEmotionView(props: {
  persona: PersonaOption;
  openingRecentSummary: string | null;
  openingMessage: string;
  busy: boolean;
  onSkip: () => void;
  onPick: (em: EmotionTag) => void;
}) {
  const { persona, openingRecentSummary, openingMessage, busy, onSkip, onPick } = props;
  return (
    <div className="flex flex-col gap-3">
      {/* recentSummary auto-load 시각 검증 */}
      <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
        <div className="mb-1 flex items-center justify-between">
          <h3 className="text-sm font-semibold">① recentSummary 자동 로드</h3>
          <span className="rounded-full bg-[var(--color-primary-soft)] px-2 py-0.5 text-[10px] text-[var(--color-primary-strong)]">
            session 응답
          </span>
        </div>
        <div className="rounded-lg border border-[var(--color-border)] bg-white/60 p-2 text-xs">
          {openingRecentSummary ?? "(직전 ENDED 세션 없음 — recentSummary 없이 인사말 생성됨)"}
        </div>
      </div>

      {/* 오프닝 메시지 (페르소나 LLM 결과) */}
      <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
        <div className="mb-1 flex items-center justify-between">
          <h3 className="text-sm font-semibold">{persona.characterName} 의 인사말</h3>
          <span className="rounded-full bg-[var(--color-surface-strong)] px-2 py-0.5 text-[10px] text-[var(--color-text-muted)]">
            HCX-005-{persona.type}
          </span>
        </div>
        <p className="whitespace-pre-wrap rounded-lg bg-white/60 p-2 text-sm leading-relaxed">
          {openingMessage}
        </p>
        <p className="mt-2 text-[10px] text-[var(--color-text-muted)]">
          위 인사말이 recentSummary 를 자연스럽게 인용/회상하는지 확인하세요.
        </p>
      </div>

      {/* pre-chat emotion */}
      <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
        <h3 className="mb-2 text-sm font-semibold">지금 감정은요?</h3>
        <p className="mb-3 text-[11px] text-[var(--color-text-muted)]">
          PATCH /api/sessions/&#123;id&#125;/pre-chat-emotion · emotion_logs 에 PRE_CHAT 저장
        </p>
        <div className="grid grid-cols-3 gap-2">
          {EMOTION_LIST.map((em) => (
            <button
              key={em}
              type="button"
              disabled={busy}
              onClick={() => onPick(em)}
              className="rounded-xl border border-[var(--color-border)] bg-white/60 px-2 py-3 text-sm hover:border-[var(--color-primary)] disabled:opacity-50"
            >
              {EMOTION_LABEL[em]}
            </button>
          ))}
        </div>
        <button
          type="button"
          disabled={busy}
          onClick={onSkip}
          className="mt-3 w-full rounded-full border border-[var(--color-border)] py-2 text-xs text-[var(--color-text-muted)] hover:bg-white/60"
        >
          건너뛰기
        </button>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────────
// Chat view
// ────────────────────────────────────────────────────────────────────────────────

function ChatView(props: {
  persona: PersonaOption;
  messages: ChatMsg[];
  input: string;
  setInput: (s: string) => void;
  busy: boolean;
  highCount: number;
  totalMsgCount: number;
  onSend: (e?: FormEvent) => void;
  onEnd: () => void;
}) {
  const {
    persona,
    messages,
    input,
    setInput,
    busy,
    highCount,
    totalMsgCount,
    onSend,
    onEnd,
  } = props;

  return (
    <div className="flex flex-1 flex-col">
      <div className="mb-2 flex items-center justify-between rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] px-4 py-3">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-sm font-semibold text-[var(--color-primary-strong)]">
            {persona.initial}
          </div>
          <div>
            <div className="text-sm font-semibold">{persona.characterName}</div>
            <div className="text-[10px] text-[var(--color-text-muted)]">
              msgs {totalMsgCount} · HIGH {highCount}
            </div>
          </div>
        </div>
        <button
          type="button"
          onClick={onEnd}
          disabled={busy}
          className="rounded-full bg-[var(--color-primary)] px-3 py-1.5 text-xs font-medium text-white hover:bg-[var(--color-primary-strong)] disabled:opacity-50"
        >
          끝내기 (요약)
        </button>
      </div>

      <div className="flex-1 overflow-y-auto rounded-2xl border border-[var(--color-border)] bg-white/60 px-4 py-4 min-h-[55vh] max-h-[60vh]">
        <div className="mb-3 text-center text-[11px] text-[var(--color-text-muted)]">
          오늘{" "}
          {formatTime(messages[0]?.createdAt ?? new Date().toISOString())}
        </div>
        <div className="flex flex-col gap-3">
          {messages.map((m) => (
            <Bubble key={m.id} message={m} personaInitial={persona.initial} />
          ))}
          {busy && (
            <div className="flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
              <span className="inline-block h-1.5 w-1.5 animate-pulse rounded-full bg-[var(--color-primary)]" />
              {persona.characterName}이(가) 답하고 있어요…
            </div>
          )}
          <div />
        </div>
      </div>

      {/* RAG 트리거 키워드 한 줄 힌트 */}
      <div className="mt-2 flex items-center gap-1 overflow-x-auto rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1.5 scrollbar-hide">
        <span className="shrink-0 text-[10px] text-[var(--color-text-muted)]">
          RAG 트리거:
        </span>
        {RAG_TRIGGER_KEYWORDS.map((k) => (
          <button
            key={k}
            type="button"
            disabled={busy}
            onClick={() => setInput(input + (input.endsWith(" ") || !input ? "" : " ") + k + " ")}
            className="shrink-0 rounded-full bg-white/60 px-2 py-0.5 text-[10px] text-[var(--color-text-muted)] hover:bg-white"
          >
            {k}
          </button>
        ))}
      </div>

      <form onSubmit={onSend} className="mt-2">
        <div className="flex items-center gap-2 rounded-[var(--radius-pill)] border border-[var(--color-border)] bg-white px-4 py-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e: KeyboardEvent<HTMLInputElement>) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                onSend();
              }
            }}
            placeholder="편하게 이야기 해주세요."
            maxLength={1000}
            disabled={busy}
            className="flex-1 bg-transparent text-sm placeholder:text-[var(--color-text-muted)] focus:outline-none disabled:opacity-60"
          />
          <button
            type="submit"
            disabled={busy || !input.trim()}
            aria-label="메시지 전송"
            className="flex h-9 w-9 items-center justify-center rounded-full bg-[var(--color-primary)] text-white hover:bg-[var(--color-primary-strong)] disabled:opacity-40"
          >
            ↑
          </button>
        </div>
      </form>
    </div>
  );
}

function Bubble({
  message,
  personaInitial,
}: {
  message: ChatMsg;
  personaInitial: string;
}) {
  const isUser = message.sender === "USER";
  const isSystem = message.sender === "SYSTEM";
  return (
    <div
      className={`flex w-full items-end gap-2 ${
        isUser ? "justify-end" : "justify-start"
      }`}
    >
      {!isUser && (
        <div
          className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
            isSystem
              ? "bg-[var(--color-warn-soft)] text-[var(--color-warn)]"
              : "bg-[var(--color-primary-soft)] text-[var(--color-primary-strong)]"
          }`}
        >
          {isSystem ? "!" : personaInitial}
        </div>
      )}
      <div className="flex max-w-[78%] flex-col items-start gap-1">
        <div
          className={`whitespace-pre-wrap rounded-[var(--radius-lg)] px-4 py-3 text-sm leading-relaxed ${
            isUser
              ? "rounded-tr-md bg-[var(--color-primary-soft)] text-[var(--color-text)]"
              : isSystem
                ? "rounded-tl-md border border-[var(--color-danger)] bg-[var(--color-danger-soft)] text-[var(--color-text)]"
                : "rounded-tl-md bg-[var(--color-surface-strong)] text-[var(--color-text)]"
          }`}
        >
          {message.content}
        </div>
        {message.meta && !isUser && (
          <div className="flex flex-wrap gap-x-2 gap-y-0.5 text-[10px] text-[var(--color-text-muted)]">
            {message.meta.aiModel && <span>model: {message.meta.aiModel}</span>}
            {message.meta.emotionTag && (
              <span>· emo: {message.meta.emotionTag}</span>
            )}
            {message.meta.riskLevel && (
              <span
                className={
                  message.meta.riskLevel === "HIGH"
                    ? "font-semibold text-[var(--color-danger)]"
                    : ""
                }
              >
                · risk: {message.meta.riskLevel}
              </span>
            )}
            {message.meta.interventionType && (
              <span>· intv: {message.meta.interventionType}</span>
            )}
            {message.meta.ragUsed && (
              <span className="rounded-full bg-[var(--color-primary-soft)] px-1.5 py-0 font-semibold text-[var(--color-primary-strong)]">
                · RAG ✓
              </span>
            )}
            {message.meta.forcedSafety && (
              <span className="font-semibold text-[var(--color-danger)]">
                · FORCED_SAFETY
              </span>
            )}
            {message.meta.showSafetyChoice && (
              <span className="font-semibold text-[var(--color-danger)]">
                · showSafetyChoice
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────────
// Summary view (HCX-007)
// ────────────────────────────────────────────────────────────────────────────────

function SummaryView(props: {
  summary: SessionEndResponse;
  emotionTally: Record<EmotionTag, number>;
  onRestart: () => void;
}) {
  const { summary, emotionTally, onRestart } = props;
  return (
    <div className="flex flex-col gap-3">
      <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-semibold">세션 요약 (HCX-007)</h2>
          <span className="rounded-full bg-[var(--color-primary-soft)] px-2 py-0.5 text-[10px] text-[var(--color-primary-strong)]">
            {summary.status} · {summary.summaryStatus}
          </span>
        </div>
        <SummaryRow label="요약" value={summary.summaryText} />
        <SummaryRow label="상황" value={summary.situationText} />
        <SummaryRow label="감정" value={summary.emotionText} />
        <SummaryRow label="사고" value={summary.thoughtText} />
        <SummaryRow label="대표 감정" value={summary.dominantEmotion ?? "-"} />
        <SummaryRow label="감정 변화" value={summary.emotionChange ?? "-"} />
        <SummaryRow label="memoryHint" value={summary.memoryHint ?? "-"} />
        <p className="mt-2 text-[10px] text-[var(--color-text-muted)]">
          memoryHint 는 다음 새 세션 생성 시 recentSummary 로 자동 주입됩니다.
          rag_chunks INSERT 가 백그라운드로 비동기 실행되어 다음 세션에서 RAG
          검색 대상이 됩니다.
        </p>
      </div>

      <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
        <h3 className="mb-2 text-sm font-semibold">FE 누적 emotionTally</h3>
        <p className="mb-2 text-[10px] text-[var(--color-text-muted)]">
          BE 의 emotion_logs 누적과 일치해야 정상. HCX-007 [세션 메타].sessionEmotionCounts 에 동일 값이 주입됨.
        </p>
        <div className="grid grid-cols-3 gap-2">
          {EMOTION_LIST.map((em) => (
            <div
              key={em}
              className="rounded-md border border-[var(--color-border)] bg-white/60 px-2 py-1 text-center text-xs"
            >
              <div className="text-[10px] text-[var(--color-text-muted)]">
                {EMOTION_LABEL[em]}
              </div>
              <div className="font-semibold">{emotionTally[em]}</div>
            </div>
          ))}
        </div>
      </div>

      <button
        type="button"
        onClick={onRestart}
        className="rounded-full bg-[var(--color-primary)] py-3 text-sm font-semibold text-white hover:bg-[var(--color-primary-strong)]"
      >
        새 세션 시작 (RAG 자동 로드 검증)
      </button>
    </div>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="mb-2 border-b border-[var(--color-border)] pb-2 last:mb-0 last:border-0 last:pb-0">
      <div className="mb-0.5 text-[10px] font-semibold text-[var(--color-text-muted)]">
        {label}
      </div>
      <div className="whitespace-pre-wrap text-sm">{value}</div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────────
// Safety overlay (PR-4 showSafetyChoice)
// ────────────────────────────────────────────────────────────────────────────────

function SafetyOverlay({
  path,
  onClose,
}: {
  path: string;
  onClose: () => void;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-[var(--color-surface)] p-5 shadow-xl">
        <div className="mb-2 inline-block rounded-full bg-[var(--color-danger-soft)] px-2 py-0.5 text-[10px] font-semibold text-[var(--color-danger)]">
          Safety Choice (PR-4)
        </div>
        <h3 className="text-base font-semibold">
          전문가 도움이 필요할 수 있어요.
        </h3>
        <p className="mt-2 text-xs text-[var(--color-text-muted)]">
          최근 대화에서 위험 신호가 누적되어 강제 안전 발화가 발화되었습니다.
          상담 센터로 이동하거나, 대화를 이어갈 수 있어요.
        </p>
        <p className="mt-2 rounded-md border border-[var(--color-border)] bg-white/60 p-2 font-mono text-[11px] text-[var(--color-text-muted)]">
          counselingCenterPath = {path}
        </p>
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            onClick={() => {
              window.alert(`(dev) 라우팅 시뮬레이션: ${path}\n실제 FE 에서는 router.push 호출.`);
              onClose();
            }}
            className="flex-1 rounded-full bg-[var(--color-danger)] py-2.5 text-xs font-semibold text-white"
          >
            상담센터 이동
          </button>
          <button
            type="button"
            onClick={onClose}
            className="flex-1 rounded-full border border-[var(--color-border)] py-2.5 text-xs text-[var(--color-text-muted)]"
          >
            대화 이어가기
          </button>
        </div>
      </div>
    </div>
  );
}
