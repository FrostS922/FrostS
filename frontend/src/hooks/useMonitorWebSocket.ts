import { useEffect, useRef, useState, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import { getRealtimeSummary, RealtimeSummary } from '@/api/monitor'

const getWsUrl = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  return `${protocol}//${host}/ws/notifications/websocket`
}

export const useMonitorWebSocket = (enabled: boolean = true) => {
  const [connected, setConnected] = useState(false)
  const [performanceData, setPerformanceData] = useState<RealtimeSummary | null>(null)
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const clientRef = useRef<Client | null>(null)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const fetchData = useCallback(async () => {
    try {
      const res = await getRealtimeSummary()
      if (res.code === 200) {
        setPerformanceData(res.data)
        setLastUpdate(new Date())
      }
    } catch {
      // ignore
    }
  }, [])

  useEffect(() => {
    if (!enabled) return

    fetchData()

    const token = localStorage.getItem('token')
    if (token) {
      const client = new Client({
        brokerURL: getWsUrl(),
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        onConnect: () => {
          setConnected(true)

          client.subscribe('/topic/perf-realtime', () => {
            fetchData()
          })

          client.subscribe('/topic/error-realtime', () => {
            fetchData()
          })

          client.subscribe('/topic/security-realtime', () => {
            fetchData()
          })
        },
        onDisconnect: () => {
          setConnected(false)
        },
        onStompError: () => {
          setConnected(false)
        },
        onWebSocketClose: () => {
          setConnected(false)
        },
      })

      client.activate()
      clientRef.current = client
    }

    intervalRef.current = setInterval(() => {
      fetchData()
    }, 30000)

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate()
        clientRef.current = null
      }
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
        intervalRef.current = null
      }
      setConnected(false)
    }
  }, [enabled, fetchData])

  return { connected, performanceData, lastUpdate }
}
