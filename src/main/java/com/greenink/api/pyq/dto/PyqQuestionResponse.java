package com.greenink.api.pyq.dto;

import com.greenink.api.pyq.PyqQuestion;

import java.util.List;

public record PyqQuestionResponse(String id, String question, List<PyqQuestion.Option> options, String source) {}
