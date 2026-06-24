package org.example.domain.episodecharacter.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.character.entity.Character;
import org.example.domain.episode.entity.Episode;
import org.example.global.common.BaseEntity;

@Entity
@Table(
    name = "episode_characters",
    uniqueConstraints = @UniqueConstraint(columnNames = {"episode_id", "character_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EpisodeCharacter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Builder
    private EpisodeCharacter(Episode episode, Character character) {
        this.episode = episode;
        this.character = character;
    }
}
