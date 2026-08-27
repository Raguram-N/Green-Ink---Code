package com.greenink.api.pyq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PyqMetadata {
    private final Map<String, Integer> totalsByUnit;
    private final int totalQuestions;

    public PyqMetadata(ObjectMapper objectMapper) throws IOException {
        MetadataFile file = objectMapper.readValue(new ClassPathResource("pyq-metadata.json").getInputStream(), MetadataFile.class);
        this.totalsByUnit = file.units().stream().collect(Collectors.toUnmodifiableMap(UnitTotal::unitId, UnitTotal::totalQuestions));
        this.totalQuestions = file.totalQuestions();
    }

    public int totalForUnit(String unitId) { return totalsByUnit.getOrDefault(unitId, 0); }
    public int totalQuestions() { return totalQuestions; }

    private record MetadataFile(java.util.List<UnitTotal> units, int totalQuestions) {}
    private record UnitTotal(String unitId, int totalQuestions) {}
}
