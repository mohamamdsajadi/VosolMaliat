package com.mparsict.vosolmaliat.common.config;

import com.mparsict.vosolmaliat.domain.enums.RelatedPersonPostCode;
import com.mparsict.vosolmaliat.domain.model.execution.RelatedPersonPost;
import com.mparsict.vosolmaliat.domain.repository.RelatedPersonPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RelatedPersonPostInitializer implements CommandLineRunner {

    private final RelatedPersonPostRepository relatedPersonPostRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (RelatedPersonPostCode code : RelatedPersonPostCode.values()) {
            if (relatedPersonPostRepository.findByCode(code).isEmpty()) {
                relatedPersonPostRepository.save(RelatedPersonPost.builder().code(code).build());
            }
        }
    }
}
