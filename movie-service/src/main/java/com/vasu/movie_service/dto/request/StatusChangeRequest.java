package com.vasu.movie_service.dto.request;

import com.vasu.movie_service.enums.EntityStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusChangeRequest {

    @NotNull(message = "Status is required")
    private EntityStatus status;
}
