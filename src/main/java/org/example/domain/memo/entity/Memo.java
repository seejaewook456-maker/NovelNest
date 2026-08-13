package org.example.domain.memo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.novel.entity.Novel;
import org.example.global.common.BaseEntity;

@Entity
@Table(name = "memos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isFavorite = false;

    @Builder
    private Memo(Novel novel, String title, String content) {
        this.novel = novel;
        this.title = title;
        this.content = content;
        this.isFavorite = false;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void updateFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }
}
