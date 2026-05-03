import { useEffect, useRef, useCallback } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import { useNotificationStore } from '../store/notificationStore'

const getWsUrl = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  return `${protocol}//${host}/api/ws/notifications/websocket`
}

export const useNotificationWebSocket = () => {
  const clientRef = useRef<Client | null>(null)
  const { fetchUnreadCount, fetchNotifications } = useNotificationStore()

  const connect = useCallback(() => {
    const token = localStorage.getItem('token')
    if (!token) return

    const client = new Client({
      brokerURL: getWsUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (message: IMessage) => {
          const data = JSON.parse(message.body)
          if (data.type === 'NEW_NOTIFICATION') {
            fetchUnreadCount()
            fetchNotifications()
          }
        })

        client.subscribe('/topic/notifications', (message: IMessage) => {
          const data = JSON.parse(message.body)
          if (data.type === 'NEW_NOTIFICATION') {
            fetchUnreadCount()
            fetchNotifications()
          }
        })
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message'])
      },
      onWebSocketClose: () => {
        console.debug('WebSocket connection closed')
      },
    })

    client.activate()
    clientRef.current = client
  }, [fetchUnreadCount, fetchNotifications])

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate()
      clientRef.current = null
    }
  }, [])

  useEffect(() => {
    connect()
    return () => disconnect()
  }, [connect, disconnect])

  return { connect, disconnect }
}
