package com.reecho.reechobe.channel.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reecho.reechobe.channel.exception.ChannelErrorCode;
import com.reecho.reechobe.common.exception.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChannelMembershipTest {

    @Test
    void 강제_제거된_멤버십은_도메인에서도_재참여할_수_없다() {
        ChannelMembership membership = ChannelMembership.join(UUID.randomUUID(), UUID.randomUUID());
        membership.remove();

        assertThatThrownBy(membership::rejoin)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ChannelErrorCode.CHANNEL_MEMBER_REMOVED);
    }
}
