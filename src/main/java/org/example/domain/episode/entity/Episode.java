package org.example.domain.episode.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.novel.entity.Novel;
import org.example.global.common.BaseEntity;

@Entity
@Table(name = "episodes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Episode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int episodeNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private Episode(Novel novel, String title, int episodeNumber, String content) {
        this.novel = novel;
        this.title = title;
        this.episodeNumber = episodeNumber;
        this.content = content;
    }

    public void update(String title, int episodeNumber, String content) {
        this.title = title;
        this.episodeNumber = episodeNumber;
        this.content = content;
    }
}
