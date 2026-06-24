# 코딩 컨벤션 및 구현 주의사항

## 1. 엔티티 삭제 시 FK 제약 순서 규칙

### 배경

Spring Data JPA의 `repository.delete(entity)`는 DB의 FK(외래키) 제약을 인식하지 못한다.
자식 테이블 레코드를 먼저 삭제하지 않으면 `DataIntegrityViolationException`이 발생하고
프론트엔드에는 "요청 처리 중 오류가 발생했습니다" 메시지가 뜬다.

### 규칙

**삭제 대상 엔티티의 자식 테이블을 모두 파악하고, 리프 노드부터 역순으로 삭제한다.**

### 현재 테이블 삭제 순서 (2026-06-24 기준)

```
Novel 삭제 시:
  1. EpisodeCharacter     (episode_id FK → Episode, character_id FK → Character)
  2. EpisodeWorldSetting  (episode_id FK → Episode, world_setting_id FK → WorldSetting)
  3. EpisodeSummary       (episode_id FK → Episode)
  4. Episode              (novel_id FK → Novel)
  5. Character            (novel_id FK → Novel)
  6. WorldSetting         (novel_id FK → Novel)
  7. Novel

Episode 삭제 시:
  1. EpisodeCharacter     (episode_id FK → Episode)
  2. EpisodeWorldSetting  (episode_id FK → Episode)
  3. EpisodeSummary       (episode_id FK → Episode)
  4. Episode

Character 삭제 시:
  1. EpisodeCharacter  (character_id FK → Character)
  2. Character

WorldSetting 삭제 시:
  1. EpisodeWorldSetting  (world_setting_id FK → WorldSetting)
  2. WorldSetting
```

### 새 엔티티 추가 시 체크리스트

새 엔티티를 만들 때 기존 엔티티를 FK로 참조하는 경우, 반드시 다음을 확인한다.

- [ ] 참조되는 부모 엔티티의 삭제 Service에 새 자식 테이블 삭제 코드 추가
- [ ] 삭제 순서가 FK 의존성 역순인지 확인
- [ ] Repository에 `deleteAllBy...` 메서드 추가

### 구현 예시

```text
// NovelService.deleteNovel
episodeCharacterRepository.deleteAllByEpisode_Novel(novel);
episodeSummaryRepository.deleteAllByEpisode_Novel(novel);
episodeRepository.deleteAllByNovel(novel);
characterRepository.deleteAllByNovel(novel);
worldSettingRepository.deleteAllByNovel(novel);
novelRepository.delete(novel);

// EpisodeService.deleteEpisode
episodeCharacterRepository.deleteAllByEpisode(episode);
episodeSummaryRepository.findByEpisode(episode).ifPresent(episodeSummaryRepository::delete);
episodeRepository.delete(episode);

// CharacterService.deleteCharacter
episodeCharacterRepository.deleteAllByCharacter(character);
characterRepository.delete(character);
```

### Spring Data JPA 네이밍 팁

중첩 프로퍼티 탐색은 언더스코어(`_`)로 표현한다.

```text
// episode.novel 경로로 Novel 기준 삭제
void deleteAllByEpisode_Novel(Novel novel);

// episode.novel 경로로 Novel 기준 조회
List<EpisodeSummary> findAllByEpisode_Novel(Novel novel);
```

---

## 2. 에러 디스패치 시 401 방지

### 배경

`JwtAuthenticationFilter`는 `OncePerRequestFilter`를 상속한다.
Spring MVC가 알 수 없는 경로를 `/error`로 에러 디스패치할 때
필터가 재실행되지 않아 SecurityContext가 비어 401이 반환된다.
프론트엔드의 `fetchWithAuth`는 401을 받으면 토큰 삭제 + 로그인 페이지로 강제 이동한다.

### 규칙

`SecurityConfig`의 `permitAll()` 목록에 `/error`를 반드시 포함한다.

```text
.requestMatchers(
    "/health", "/error",          // ← /error 필수
    "/api/users/signup", "/api/users/login",
    ...
).permitAll()
```

---

## 3. GlobalExceptionHandler 미처리 예외 → 500

### 배경

`GlobalExceptionHandler`에 등록되지 않은 예외가 발생하면
Spring Boot의 BasicErrorController가 처리하며 `ApiResponse` 형식이 아닌 응답을 반환한다.
프론트엔드 `fetchWithAuth`의 `json.message`가 `undefined`가 되어
"요청 처리 중 오류가 발생했습니다" 메시지가 뜬다.

### 규칙

서비스에서 새로운 종류의 예외를 던질 경우 `GlobalExceptionHandler`에 핸들러를 추가한다.

```text
// 현재 등록된 핸들러
@ExceptionHandler(IllegalArgumentException.class)  → 400
@ExceptionHandler(SecurityException.class)          → 403
@ExceptionHandler(MethodArgumentNotValidException)  → 400
@ExceptionHandler(HttpMessageNotReadableException)  → 400
```

---

## 4. 프론트엔드 fetchWithAuth 동작 원칙

| 서버 응답     | fetchWithAuth 동작                                          |
|--------------|-------------------------------------------------------------|
| 200~299      | 정상 반환                                                    |
| 401          | `removeToken()` + `/login` 강제 이동 (catch로 막을 수 없음) |
| 400, 403, 500 | `json.message` 또는 기본 메시지로 Error throw               |

**401은 catch로 막아도 이미 리다이렉트가 실행된 뒤다.**
불필요한 401이 발생하지 않도록 서버 설정을 정확히 유지한다.
