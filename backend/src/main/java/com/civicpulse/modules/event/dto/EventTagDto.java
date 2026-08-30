package com.civicpulse.modules.event.dto;

import com.civicpulse.modules.event.model.EventTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTagDto {

    private UUID id;
    private String name;
    private String slug;

    public static EventTagDto fromEntity(EventTag tag) {
        if (tag == null) return null;
        return EventTagDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .build();
    }
}
