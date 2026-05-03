/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ERROR_REPORT_ENABLED?: string
  readonly VITE_PERF_SAMPLING_RATE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

interface Window {
  __FROSTS_TRACE_ID__?: string
}
