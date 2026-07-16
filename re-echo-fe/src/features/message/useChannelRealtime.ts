import { Client } from '@stomp/stompjs'
import { useEffect, useRef } from 'react'
import { getAccessToken } from '../../stores/authStore'
import type { ChannelMessage } from './messageApi'

type RealtimeEvent =
  | {
      eventId: string
      type: 'MESSAGE_CREATED' | 'MESSAGE_UPDATED' | 'MESSAGE_DELETED'
      occurredAt: string
      payload: { message: ChannelMessage }
    }
  | {
      eventId: string
      type: 'TYPING_UPDATED'
      occurredAt: string
      payload: { typingUserIds: string[] }
    }

interface ChannelRealtimeOptions {
  workspaceId: string
  channelId: string
  enabled: boolean
  onEvent: (event: RealtimeEvent) => void
}

// 채널 STOMP 구독을 유지하고 중복 event를 제거한 뒤 화면에 전달한다.
export function useChannelRealtime({
  workspaceId,
  channelId,
  enabled,
  onEvent,
}: ChannelRealtimeOptions) {
  const clientRef = useRef<Client | null>(null)
  const eventIdsRef = useRef(new Set<string>())
  const onEventRef = useRef(onEvent)
  onEventRef.current = onEvent

  useEffect(() => {
    if (!enabled || !workspaceId || !channelId) {
      return undefined
    }
    const client = new Client({
      brokerURL: getBrokerUrl(),
      reconnectDelay: 3000,
      beforeConnect: () => {
        const accessToken = getAccessToken()
        client.connectHeaders = accessToken ? { Authorization: `Bearer ${accessToken}` } : {}
      },
    })
    client.onConnect = () => {
      client.subscribe(`/sub/workspaces/${workspaceId}/channels/${channelId}`, (frame) => {
        const event = JSON.parse(frame.body) as RealtimeEvent
        if (eventIdsRef.current.has(event.eventId)) {
          return
        }
        eventIdsRef.current.add(event.eventId)
        if (eventIdsRef.current.size > 200) {
          const firstEventId = eventIdsRef.current.values().next().value
          if (firstEventId) {
            eventIdsRef.current.delete(firstEventId)
          }
        }
        onEventRef.current(event)
      })
    }
    client.activate()
    clientRef.current = client
    return () => {
      clientRef.current = null
      void client.deactivate()
    }
  }, [channelId, enabled, workspaceId])

  function publishTyping(typing: boolean) {
    if (!clientRef.current?.connected) {
      return
    }
    clientRef.current.publish({
      destination: `/pub/workspaces/${workspaceId}/channels/${channelId}/typing`,
      body: JSON.stringify({ typing }),
    })
  }

  return { publishTyping }
}

function getBrokerUrl() {
  const configuredUrl = import.meta.env.VITE_WS_URL
  if (configuredUrl) {
    return configuredUrl
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}
