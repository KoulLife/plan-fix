# 공동 코스 초대 API

카카오톡 공유 버튼은 프론트에서 `inviteUrl`을 카카오 공유 메시지의 링크로 넣으면 된다.
초대 링크는 생성 시점부터 7일간 유효하며 같은 링크로 여러 사용자가 참여할 수 있다.

## 초대 링크 생성

`POST /api/v1/courses/{courseId}/invites`

소유자만 호출할 수 있다. `memberRole`은 `VIEWER` 또는 `EDITOR`다.

```json
{ "memberRole": "EDITOR" }
```

```json
{
  "token": "...",
  "inviteUrl": "https://frontend.example.com/course-invites/...",
  "memberRole": "EDITOR",
  "expiresAt": "2026-09-17T10:00:00Z"
}
```

## 초대 링크 미리보기

`GET /api/v1/course-invites/{token}`

인증 없이 호출 가능하다. 링크를 연 비회원 화면에서 코스 제목과 권한을 보여주고,
회원가입 또는 로그인으로 유도한다.

## 초대 수락

`POST /api/v1/course-invites/{token}/accept`

로그인이 필요하다. 비회원 요청은 `401 Unauthorized`이므로 가입/로그인 완료 후 같은 요청을
재시도한다. 이미 참여한 사용자가 다시 호출해도 성공 응답을 돌려준다.

## 멤버 관리

- `GET /api/v1/courses/{courseId}/members` — 소유자만 멤버 목록 조회
- `DELETE /api/v1/courses/{courseId}/members/{memberUserId}` — 소유자만 멤버 내보내기

`VIEWER`는 조회만 가능하고, `EDITOR`는 기존 `PATCH /api/v1/courses/{courseId}`로 일정 수정이 가능하다.
코스 삭제·멤버 관리는 소유자만 가능하다.
