package com.reecho.reechobe.message.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.reecho.reechobe.message.domain.Message;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageCommandServiceTest {

    @Test
    void 첨부_전용_메시지는_NOT_NULL_본문_컬럼에_빈_문자열로_저장한다() {
        String content = MessageCommandService.normalizeContent(null);

        Message message = Message.create(UUID.randomUUID(), UUID.randomUUID(), content);

        assertThat(message.getContent()).isEmpty();
    }
}
