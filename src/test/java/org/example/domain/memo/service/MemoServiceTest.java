package org.example.domain.memo.service;

import org.example.domain.memo.dto.MemoCreateRequestDto;
import org.example.domain.memo.dto.MemoFavoriteRequestDto;
import org.example.domain.memo.dto.MemoResponseDto;
import org.example.domain.memo.dto.MemoUpdateRequestDto;
import org.example.domain.memo.entity.Memo;
import org.example.domain.memo.repository.MemoRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.novel.repository.NovelRepository;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

    @Mock
    private MemoRepository memoRepository;
    @Mock
    private NovelRepository novelRepository;
    @Mock
    private UserRepository userRepository;

    private MemoService memoService;

    private static final String EMAIL = "writer@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    private User owner;
    private User other;
    private Novel novel;

    @BeforeEach
    void setUp() {
        memoService = new MemoService(memoRepository, novelRepository, userRepository);

        owner = User.builder().email(EMAIL).password("encoded").nickname("작가").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(owner, "id", 1L);

        other = User.builder().email(OTHER_EMAIL).password("encoded").nickname("남").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(other, "id", 2L);

        novel = Novel.builder().user(owner).title("테스트 작품").genre("판타지").description("설명").build();
        ReflectionTestUtils.setField(novel, "id", 10L);
    }

    private Memo memo(Novel novel, String title, String content, Long id) {
        Memo memo = Memo.builder().novel(novel).title(title).content(content).build();
        ReflectionTestUtils.setField(memo, "id", id);
        return memo;
    }

    private MemoCreateRequestDto createRequest(String title, String content) {
        MemoCreateRequestDto dto = new MemoCreateRequestDto();
        ReflectionTestUtils.setField(dto, "title", title);
        ReflectionTestUtils.setField(dto, "content", content);
        return dto;
    }

    private MemoUpdateRequestDto updateRequest(String title, String content) {
        MemoUpdateRequestDto dto = new MemoUpdateRequestDto();
        ReflectionTestUtils.setField(dto, "title", title);
        ReflectionTestUtils.setField(dto, "content", content);
        return dto;
    }

    private MemoFavoriteRequestDto favoriteRequest(boolean isFavorite) {
        return new MemoFavoriteRequestDto(isFavorite);
    }

    @Test
    void 메모를_생성한다() {
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(novelRepository.findById(10L)).willReturn(Optional.of(novel));
        given(memoRepository.save(any(Memo.class))).willAnswer(invocation -> {
            Memo saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        MemoResponseDto response = memoService.createMemo(EMAIL, 10L, createRequest("30화 복선 정리", "레온의 반지"));

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getNovelId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("30화 복선 정리");
        assertThat(response.getContent()).isEqualTo("레온의 반지");
        assertThat(response.getIsFavorite()).isFalse();
    }

    @Test
    void 한_작품에_여러_메모를_생성할_수_있다() {
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(novelRepository.findById(10L)).willReturn(Optional.of(novel));
        given(memoRepository.save(any(Memo.class))).willAnswer(invocation -> invocation.getArgument(0));

        memoService.createMemo(EMAIL, 10L, createRequest("메모1", "내용1"));
        memoService.createMemo(EMAIL, 10L, createRequest("메모2", "내용2"));

        verify(memoRepository, org.mockito.Mockito.times(2)).save(any(Memo.class));
    }

    @Test
    void 작품의_메모_목록을_조회한다() {
        Memo m1 = memo(novel, "메모1", "내용1", 100L);
        Memo m2 = memo(novel, "메모2", "내용2", 101L);
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(novelRepository.findById(10L)).willReturn(Optional.of(novel));
        // 즐겨찾기 우선 정렬은 Repository 쿼리 메서드가 담당 — Service는 순서를 그대로 반환하는지만 검증
        given(memoRepository.findAllByNovelOrderByIsFavoriteDescUpdatedAtDesc(novel)).willReturn(List.of(m2, m1));

        List<MemoResponseDto> result = memoService.getMemos(EMAIL, 10L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MemoResponseDto::getId).containsExactly(101L, 100L);
    }

    @Test
    void 메모_상세를_조회한다() {
        Memo m = memo(novel, "메모1", "내용1", 100L);
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(memoRepository.findById(100L)).willReturn(Optional.of(m));

        MemoResponseDto result = memoService.getMemo(EMAIL, 100L);

        assertThat(result.getTitle()).isEqualTo("메모1");
        assertThat(result.getContent()).isEqualTo("내용1");
    }

    @Test
    void 존재하지_않는_메모를_조회하면_예외가_발생한다() {
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(memoRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memoService.getMemo(EMAIL, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("메모를 찾을 수 없습니다");
    }

    @Test
    void 메모_제목과_내용을_수정한다() {
        Memo m = memo(novel, "원래 제목", "원래 내용", 100L);
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(memoRepository.findById(100L)).willReturn(Optional.of(m));

        MemoResponseDto result = memoService.updateMemo(EMAIL, 100L, updateRequest("수정된 제목", "수정된 내용"));

        assertThat(result.getTitle()).isEqualTo("수정된 제목");
        assertThat(result.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    void 메모를_즐겨찾기로_설정한다() {
        Memo m = memo(novel, "메모1", "내용1", 100L);
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(memoRepository.findById(100L)).willReturn(Optional.of(m));

        MemoResponseDto result = memoService.toggleFavorite(EMAIL, 100L, favoriteRequest(true));

        assertThat(result.getIsFavorite()).isTrue();
        assertThat(m.getIsFavorite()).isTrue();
    }

    @Test
    void 메모_즐겨찾기를_해제한다() {
        Memo m = memo(novel, "메모1", "내용1", 100L);
        m.updateFavorite(true);
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(memoRepository.findById(100L)).willReturn(Optional.of(m));

        MemoResponseDto result = memoService.toggleFavorite(EMAIL, 100L, favoriteRequest(false));

        assertThat(result.getIsFavorite()).isFalse();
    }

    @Test
    void 다른_사용자의_메모_즐겨찾기를_변경하면_권한_예외가_발생하고_변경되지_않는다() {
        Memo m = memo(novel, "메모1", "내용1", 100L);
        given(userRepository.findByEmailAndDeletedAtIsNull(OTHER_EMAIL)).willReturn(Optional.of(other));
        given(memoRepository.findById(100L)).willReturn(Optional.of(m));

        assertThatThrownBy(() -> memoService.toggleFavorite(OTHER_EMAIL, 100L, favoriteRequest(true)))
                .isInstanceOf(SecurityException.class);

        assertThat(m.getIsFavorite()).isFalse();
    }

    @Test
    void 메모를_삭제한다() {
        Memo m = memo(novel, "메모1", "내용1", 100L);
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(memoRepository.findById(100L)).willReturn(Optional.of(m));

        memoService.deleteMemo(EMAIL, 100L);

        verify(memoRepository).delete(m);
    }

    @Test
    void 다른_사용자의_메모를_조회하면_권한_예외가_발생한다() {
        Memo m = memo(novel, "메모1", "내용1", 100L);
        given(userRepository.findByEmailAndDeletedAtIsNull(OTHER_EMAIL)).willReturn(Optional.of(other));
        given(memoRepository.findById(100L)).willReturn(Optional.of(m));

        assertThatThrownBy(() -> memoService.getMemo(OTHER_EMAIL, 100L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("권한이 없습니다");
    }

    @Test
    void 다른_사용자의_메모를_수정하면_권한_예외가_발생하고_수정되지_않는다() {
        Memo m = memo(novel, "원래 제목", "원래 내용", 100L);
        given(userRepository.findByEmailAndDeletedAtIsNull(OTHER_EMAIL)).willReturn(Optional.of(other));
        given(memoRepository.findById(100L)).willReturn(Optional.of(m));

        assertThatThrownBy(() -> memoService.updateMemo(OTHER_EMAIL, 100L, updateRequest("해킹 시도", "해킹 내용")))
                .isInstanceOf(SecurityException.class);

        assertThat(m.getTitle()).isEqualTo("원래 제목");
    }

    @Test
    void 다른_사용자의_메모를_삭제하면_권한_예외가_발생하고_삭제되지_않는다() {
        Memo m = memo(novel, "메모1", "내용1", 100L);
        given(userRepository.findByEmailAndDeletedAtIsNull(OTHER_EMAIL)).willReturn(Optional.of(other));
        given(memoRepository.findById(100L)).willReturn(Optional.of(m));

        assertThatThrownBy(() -> memoService.deleteMemo(OTHER_EMAIL, 100L))
                .isInstanceOf(SecurityException.class);

        verify(memoRepository, never()).delete(any(Memo.class));
    }

    @Test
    void 다른_작품의_메모_목록에는_접근할_수_없다() {
        Novel otherNovel = Novel.builder().user(other).title("남의 작품").genre("로맨스").description("설명").build();
        ReflectionTestUtils.setField(otherNovel, "id", 20L);

        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(novelRepository.findById(20L)).willReturn(Optional.of(otherNovel));

        assertThatThrownBy(() -> memoService.getMemos(EMAIL, 20L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("권한이 없습니다");
    }
}
