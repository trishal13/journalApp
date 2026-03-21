package com.trishal.journalApp.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;

/**
 * Minimal mapping of the Gemini generateContent response.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {

    private List<Candidate> candidates;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        private String text;
    }

    /**
     * Convenience method: returns the raw text from the first candidate/part.
     */
    public String getFirstText() {
        if (!ObjectUtils.isEmpty(candidates)) {
            Candidate candidate = candidates.get(0);
            if (!ObjectUtils.isEmpty(candidate.getContent())
                    && !ObjectUtils.isEmpty(candidate.getContent().getParts())) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return null;
    }
}