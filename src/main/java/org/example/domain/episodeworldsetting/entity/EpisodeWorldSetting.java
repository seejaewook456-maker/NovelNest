package org.example.domain.episodeworldsetting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.episode.entity.Episode;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.global.common.BaseEntity;

@Entity
@Table(
    name = "episode_world_settings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"episode_id", "world_setting_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EpisodeWorldSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_setting_id", nullable = false)
    private WorldSetting worldSetting;

    @Builder
    private EpisodeWorldSetting(Episode episode, WorldSetting worldSetting) {
        this.episode = episode;
        this.worldSetting = worldSetting;
    }
}
