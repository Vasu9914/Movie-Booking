package com.vasu.movie_service.dto.request;

import com.vasu.movie_service.enums.ShowStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowStatusChangeRequest {

    @NotNull(message = "Status is required")
    private ShowStatus status;
}
