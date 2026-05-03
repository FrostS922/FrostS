import { message } from 'antd'
import errorReporter from './errorReporter'

const showError = (err: any, fallback: string) => {
  const serverMessage = err?.response?.data?.message
  message.error(serverMessage || fallback)
  errorReporter.report(err, fallback)
}

export default showError
