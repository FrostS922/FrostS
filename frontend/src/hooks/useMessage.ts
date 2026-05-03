import { App } from 'antd'
import errorReporter from '@/utils/errorReporter'

const useMessage = () => {
  const { message } = App.useApp()

  const showError = (err: any, fallback: string) => {
    const serverMessage = err?.response?.data?.message
    message.error(serverMessage || fallback)
    errorReporter.report(err, fallback)
  }

  return { message, showError }
}

export default useMessage
