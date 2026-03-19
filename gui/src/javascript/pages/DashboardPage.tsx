import {CCol, CContainer, CRow, CSpinner} from '@coreui/react'
import {useTranslation} from 'react-i18next'
import {CreateStoreCard, DetailStoreCard} from '../components'
import {useManagerStatus} from '../auth/useManagerStatus'
import {useProfile} from '../auth/useProfile'
import { useManagerApi } from '../api/ManagerApiProvider'
import { useState } from 'react'
import { selectPreferredStoreApp } from '../utils/storeApp'

type DashboardPageProps = {
  onNotify: (message: string) => void
}

export function DashboardPage({ onNotify }: DashboardPageProps) {
  const { t } = useTranslation()
  const { apps, hasStore, reload: reloadStatus, isLoading } = useManagerStatus()
  const { profile } = useProfile()
  const managerApi = useManagerApi()

  const [creationBusy, setCreationBusy] = useState(false)

  const storeApp = selectPreferredStoreApp(apps)

  const handleCreateStore = async () => {
    if (!profile?.id) return
    try {
      setCreationBusy(true)
      const name = profile.username ?? profile.id
      await managerApi.createApp('DOCKER_STORE', name)
      onNotify(t('dashboard.storeCreated'))
    } catch (error) {
      console.error('Failed to create store:', error)
      onNotify(t('dashboard.storeCreationFailed'))
    } finally {
      setCreationBusy(false)
    }
  }

  return (
    <CContainer fluid className="py-3">
      {/* remove global centering so detail card can stay left; CreateStoreCard keeps mx-auto */}
      <CRow className="g-3">
        {/* while we are loading the manager status, show a loader */}
        {isLoading ? (
          <CCol xs={12} className="d-flex justify-content-center">
            <CSpinner />
          </CCol>
        ) : (
          // une fois chargé, afficher soit la card de création soit la card de détail
          <>
            {!hasStore && (
              <CCol xs={12}>
                <CreateStoreCard
                  onCreate={handleCreateStore}
                  disabled={creationBusy}
                  busy={creationBusy}
                />
              </CCol>
            )}

            {hasStore && storeApp && (
              <CCol xs={12}>
                {/* wrapper fixe pour limiter largeur et aligner à gauche */}
                <div style={{ maxWidth: 1200 }}>
                  <DetailStoreCard
                    app={storeApp}
                    onRefresh={() => reloadStatus()}
                    onDeleted={() => {
                      void reloadStatus()
                      onNotify(t('dashboard.storeDeleted'))
                    }}
                  />
                </div>
              </CCol>
            )}
          </>
        )}
      </CRow>
    </CContainer>
  )
}
