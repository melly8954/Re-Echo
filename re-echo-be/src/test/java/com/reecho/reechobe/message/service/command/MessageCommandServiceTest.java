package com.reecho.reechobe.message.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.reecho.reechobe.message.domain.Message;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageCommandServiceTest {

    @Test
    void 관리_권한자가_타인_메시지를_삭제하면_관리_삭제로_구분한다() {
        UUID authorMembershipId = UUID.randomUUID();
        Message message = Message.create(UUID.randomUUID(), authorMembershipId, "메시지");

        message.delete(UUID.randomUUID());

        assertThat(message.isModeratorDeleted()).isTrue();
    }

    @Test
    void 작성자가_본인_메시지를_삭제하면_관리_삭제로_구분하지_않는다() {
        UUID authorMembershipId = UUID.randomUUID();
        Message message = Message.create(UUID.randomUUID(), authorMembershipId, "메시지");

        message.delete(authorMembershipId);

        assertThat(message.isModeratorDeleted()).isFalse();
    }

    @Test
    void 첨부_전용_메시지는_NOT_NULL_본문_컬럼에_빈_문자열로_저장한다() {
        String content = MessageCommandService.normalizeContent(null);

        Message message = Message.create(UUID.randomUUID(), UUID.randomUUID(), content);

        assertThat(message.getContent()).isEmpty();
    }
}
