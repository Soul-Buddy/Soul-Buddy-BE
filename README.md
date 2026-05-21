# frontend_jihun_temp

Soul Buddy AI 채팅 파이프라인을 단일 페이지에서 테스트하기 위한 **임시** Next.js 14 프로젝트.

> 실제 사용자용 frontend는 `../frontend/` 폴더입니다. 이 폴더는 백엔드/CLOVA 연동을 빠르게 검증하기 위한 용도이며 프로덕션에서 사용되지 않습니다.

## 흐름

1. dev-login (페르소나 선택) → JWT 발급 + 약관 동의 + 자동 온보딩
2. 페르소나 카드에서 포코/루미 선택 → `POST /api/sessions` 호출, 백엔드가 HCX-005로 오프닝 메시지 생성
3. 메시지 입력 → `POST /api/chat` 호출 → DASH-002 분류 + Safety Gate + HCX-005 응답
4. (선택) 세션 종료 버튼으로 `PATCH /api/sessions/{id}/end` 호출 → HCX-007 요약

dev-login은 backend의 `@Profile("local")` 가드로 보호되며 `local` 프로필이 아닐 경우 빈으로 등록되지 않는다.

## 실행

백엔드를 먼저 띄운 뒤 (`backend/.env` 준비 후 `./gradlew bootRun`), 다음 명령으로 프론트 dev 서버를 시작한다:

```bash
npm install   # 최초 1회
npm run dev   # http://localhost:3000
```

`.env.local`에서 백엔드 주소를 바꿀 수 있다 (`NEXT_PUBLIC_API_BASE_URL`).

## 인증 토큰

`localStorage["soulbuddy.devToken"]`에 보관된다. "로그아웃" 버튼으로 비울 수 있다.
