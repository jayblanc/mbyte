import {useState, useEffect} from 'react'
import {useTranslation} from 'react-i18next'
import {Navigate, Route, Routes} from 'react-router-dom'
import './App.css'
import {RequireAuth} from './auth/RequireAuth'
import {DashboardPage} from './pages/DashboardPage'
import {NotFoundPage} from './pages/NotFoundPage'
import {StorePage} from './pages/StorePage'
import {Header, SideBar} from './components'
import {CToast, CToastBody, CToastHeader,} from '@coreui/react'
import {useWebSocket} from './utils/useWebSocket'
import WebhooksPage from "./components/webhook/WebhooksManagement.tsx";

export default function App() {
  const { t } = useTranslation()
  const [sidebarNarrow, setSidebarNarrow] = useState(false)
  const [toastMessage, setToastMessage] = useState('')
  const [showToast, setShowToast] = useState(false)

  // Connect to WebSocket for notifications
  useWebSocket('/notifications')

  const handleNotify = (message: string) => {
    setToastMessage(message)
    setShowToast(true)
  }

  useEffect(() => {
    const onGlobalToast = (ev: Event) => {
      // Expect a CustomEvent with detail { message }
      const ce = ev as CustomEvent<{ message?: string }>
      const msg = ce?.detail?.message ?? String((ev as any).detail ?? '')
      if (msg) {
        setToastMessage(String(msg))
        setShowToast(true)
      }
    }
    globalThis.addEventListener('mbyte-toast', onGlobalToast as EventListener)
    return () => globalThis.removeEventListener('mbyte-toast', onGlobalToast as EventListener)
  }, [])

  return (
    <RequireAuth>
      <div className="min-vh-100 d-flex">
        <SideBar narrow={sidebarNarrow} />

        <div className="flex-grow-1 d-flex flex-column">
          <Header onToggleSidebar={() => setSidebarNarrow((v) => !v)} />

          <main className="flex-grow-1 position-relative">
            <Routes>
              <Route path="/" element={
                  <Navigate to="/dashboard" replace />}
              />
              <Route path="/dashboard" element={
                  <DashboardPage onNotify={handleNotify} />}
              />
              <Route path="/s/:index/*" element={
                  <StorePage />
                }
              />
              <Route path="/webhooks" element={
                <WebhooksPage/>
              }/>
              <Route path="*" element={<NotFoundPage />} />
            </Routes>

            {showToast && (
              <div style={{ position: 'fixed', right: 16, top: 16, zIndex: 2000 }}>
                <CToast autohide visible onClose={() => setShowToast(false)}>
                  <CToastHeader closeButton>
                    <strong className="me-auto">{t('common.appName')}</strong>
                    <small>{t('common.now')}</small>
                  </CToastHeader>
                  <CToastBody>{toastMessage}</CToastBody>
                </CToast>
              </div>
            )}
          </main>
        </div>
      </div>
    </RequireAuth>
  )
}
