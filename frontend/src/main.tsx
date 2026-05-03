import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './index.css'
import AppThemeProvider from './theme/AppThemeProvider'
import errorReporter from './utils/errorReporter'

const originalError = console.error
console.error = (...args) => {
  if (typeof args[0] === 'string' && args[0].includes('findDOMNode is deprecated')) {
    return
  }
  originalError.call(console, ...args)
}

errorReporter.installGlobalHandlers()

function initWebVitals() {
  import('web-vitals').then(({ onLCP, onCLS, onTTFB, onINP, onFCP }) => {
    onLCP((metric: { name: string; value: number; rating?: string; delta?: number; navigationType?: string }) => errorReporter.reportWebVital(metric))
    onCLS((metric: { name: string; value: number; rating?: string; delta?: number; navigationType?: string }) => errorReporter.reportWebVital(metric))
    onTTFB((metric: { name: string; value: number; rating?: string; delta?: number; navigationType?: string }) => errorReporter.reportWebVital(metric))
    onINP((metric: { name: string; value: number; rating?: string; delta?: number; navigationType?: string }) => errorReporter.reportWebVital(metric))
    onFCP((metric: { name: string; value: number; rating?: string; delta?: number; navigationType?: string }) => errorReporter.reportWebVital(metric))
  }).catch(() => {})
}

initWebVitals()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <AppThemeProvider>
      <App />
    </AppThemeProvider>
  </BrowserRouter>,
)
