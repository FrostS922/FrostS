import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './index.css'
import AppThemeProvider from './theme/AppThemeProvider'

// Suppress known third-party warnings
const originalError = console.error
console.error = (...args) => {
  if (typeof args[0] === 'string' && args[0].includes('findDOMNode is deprecated')) {
    return
  }
  originalError.call(console, ...args)
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <AppThemeProvider>
      <App />
    </AppThemeProvider>
  </BrowserRouter>,
)
