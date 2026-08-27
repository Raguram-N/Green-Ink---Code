package com.greenink.api.pyq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PyqAnswerRequest(
        @NotBlank String questionId,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]+") String selectedOption
) {}
