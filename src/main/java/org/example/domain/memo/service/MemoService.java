package org.example.domain.memo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.memo.dto.MemoCreateRequestDto;
import org.example.domain.memo.dto.MemoFavoriteRequestDto;
import org.example.domain.memo.dto.MemoResponseDto;
import org.example.domain.memo.dto.MemoUpdateRequestDto;
import org.example.domain.memo.entity.Memo;
import org.example.domain.memo.repository.MemoRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.novel.repository.NovelRepository;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;
    private final NovelRepository novelRepository;
    private final UserRepository userRepository;

    @Transactional
    public MemoResponseDto createMemo(String email, Long novelId, MemoCreateRequestDto dto) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        Memo memo = Memo.builder()
                .novel(novel)
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();

        Memo saved = memoRepository.save(memo);
        log.info("Memo created. memoId={}, novelId={}, userId={}", saved.getId(), novelId, user.getId());
        return MemoResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<MemoResponseDto> getMemos(String email, Long novelId) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        return memoRepository.findAllByNovelOrderByIsFavoriteDescUpdatedAtDesc(novel).stream()
                .map(MemoResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemoResponseDto getMemo(String email, Long memoId) {
        User user = findUserByEmail(email);
        Memo memo = findMemoById(memoId);
        validateOwner(memo.getNovel(), user);

        return MemoResponseDto.from(memo);
    }

    @Transactional
    public MemoResponseDto updateMemo(String email, Long memoId, MemoUpdateRequestDto dto) {
        User user = findUserByEmail(email);
        Memo memo = findMemoById(memoId);
        validateOwner(memo.getNovel(), user);

        memo.update(dto.getTitle(), dto.getContent());
        log.info("Memo updated. memoId={}, userId={}", memoId, user.getId());
        return MemoResponseDto.from(memo);
    }

    @Transactional
    public MemoResponseDto toggleFavorite(String email, Long memoId, MemoFavoriteRequestDto dto) {
        User user = findUserByEmail(email);
        Memo memo = findMemoById(memoId);
        validateOwner(memo.getNovel(), user);

        memo.updateFavorite(dto.getIsFavorite());
        return MemoResponseDto.from(memo);
    }

    @Transactional
    public void deleteMemo(String email, Long memoId) {
        User user = findUserByEmail(email);
        Memo memo = findMemoById(memoId);
        validateOwner(memo.getNovel(), user);

        memoRepository.delete(memo);
        log.info("Memo deleted. memoId={}, userId={}", memoId, user.getId());
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Novel findNovelById(Long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("작품을 찾을 수 없습니다."));
    }

    private Memo findMemoById(Long memoId) {
        return memoRepository.findById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("메모를 찾을 수 없습니다."));
    }

    // novel.getUser()를 통해 소유자 검증 — Memo는 Novel을 통해 User에 도달
    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 작품에 대한 권한이 없습니다.");
        }
    }
}
