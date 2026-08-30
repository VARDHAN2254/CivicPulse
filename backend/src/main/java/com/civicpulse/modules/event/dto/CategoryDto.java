package com.civicpulse.modules.event.dto;

import com.civicpulse.modules.event.model.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String iconName;
    private int displayOrder;

    public static CategoryDto fromEntity(Category category) {
        if (category == null) return null;
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .iconName(category.getIconName())
                .displayOrder(category.getDisplayOrder())
                .build();
    }
}
