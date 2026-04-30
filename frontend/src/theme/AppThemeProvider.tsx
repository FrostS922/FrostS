import React, { useEffect, useMemo } from 'react'
import { ConfigProvider, theme as antdTheme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { useThemeStore } from '../store/themeStore'

interface AppThemeProviderProps {
  children: React.ReactNode
}

const AppThemeProvider: React.FC<AppThemeProviderProps> = ({ children }) => {
  const mode = useThemeStore((state) => state.mode)
  const isDark = mode === 'dark'

  useEffect(() => {
    document.documentElement.dataset.theme = mode
    document.documentElement.style.colorScheme = mode
  }, [mode])

  const themeConfig = useMemo(
    () => ({
      algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
      token: {
        colorPrimary: isDark ? '#2dd4bf' : '#0f9f8f',
        colorInfo: isDark ? '#7aa2ff' : '#3f6fd8',
        colorSuccess: isDark ? '#58d68d' : '#1f9d55',
        colorWarning: isDark ? '#f3b85b' : '#bd7c17',
        colorError: isDark ? '#ff6b81' : '#d9364f',
        colorBgBase: isDark ? '#101317' : '#f5f7f9',
        colorTextBase: isDark ? '#e7edf3' : '#17202a',
        colorBorder: isDark ? '#2a333d' : '#dce4ec',
        colorBorderSecondary: isDark ? '#232b33' : '#e8eef3',
        borderRadius: 8,
        controlHeight: 36,
        fontFamily:
          '"Segoe UI Variable", "Aptos", "Microsoft YaHei", "PingFang SC", sans-serif',
      },
      components: {
        Layout: {
          bodyBg: isDark ? '#101317' : '#f5f7f9',
          headerBg: isDark ? '#14181d' : '#ffffff',
          siderBg: isDark ? '#14181d' : '#ffffff',
        },
        Menu: {
          itemBg: 'transparent',
          subMenuItemBg: 'transparent',
          darkItemBg: 'transparent',
          darkSubMenuItemBg: 'transparent',
          itemSelectedBg: isDark ? 'rgba(45, 212, 191, 0.16)' : 'rgba(15, 159, 143, 0.12)',
          itemSelectedColor: isDark ? '#7ff5e5' : '#087a70',
          darkItemSelectedBg: isDark ? 'rgba(45, 212, 191, 0.16)' : 'rgba(15, 159, 143, 0.12)',
          darkItemSelectedColor: isDark ? '#7ff5e5' : '#087a70',
          itemColor: isDark ? '#a8b3bf' : '#40505f',
          darkItemColor: isDark ? '#a8b3bf' : '#40505f',
          itemHoverColor: isDark ? '#f5fbff' : '#17202a',
          darkItemHoverColor: isDark ? '#f5fbff' : '#17202a',
          itemBorderRadius: 8,
        },
        Card: {
          colorBgContainer: isDark ? '#171c22' : '#ffffff',
          colorBorderSecondary: isDark ? '#25303a' : '#e2e9f0',
          borderRadiusLG: 8,
        },
        Table: {
          headerBg: isDark ? '#1c232a' : '#f1f5f8',
          headerColor: isDark ? '#dbe6ef' : '#26323d',
          rowHoverBg: isDark ? 'rgba(45, 212, 191, 0.08)' : 'rgba(15, 159, 143, 0.06)',
          borderColor: isDark ? '#26303a' : '#e2e9f0',
        },
        Button: {
          borderRadius: 8,
          controlHeight: 36,
          primaryShadow: 'none',
        },
        Input: {
          borderRadius: 8,
          activeShadow: isDark
            ? '0 0 0 3px rgba(45, 212, 191, 0.12)'
            : '0 0 0 3px rgba(15, 159, 143, 0.12)',
        },
        Select: {
          borderRadius: 8,
        },
        Modal: {
          borderRadiusLG: 8,
          contentBg: isDark ? '#171c22' : '#ffffff',
          headerBg: isDark ? '#171c22' : '#ffffff',
        },
        Tabs: {
          itemSelectedColor: isDark ? '#7ff5e5' : '#087a70',
          inkBarColor: isDark ? '#2dd4bf' : '#0f9f8f',
        },
        Statistic: {
          titleFontSize: 13,
        },
      },
    }),
    [isDark],
  )

  return (
    <ConfigProvider locale={zhCN} theme={themeConfig}>
      {children}
    </ConfigProvider>
  )
}

export default AppThemeProvider
