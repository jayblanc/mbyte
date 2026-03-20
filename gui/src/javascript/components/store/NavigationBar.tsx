import { CButton } from '@coreui/react'
import { CIcon } from '@coreui/icons-react'
import { cilGrid, cilList, cilHome, cilInfo, cilFolder, cilCloudUpload, cilHistory } from '@coreui/icons'
import { useTranslation } from 'react-i18next'

type BreadcrumbItemInline = { id?: string, name: string }

type NavigationBarProps = Readonly<{
  breadcrumb: BreadcrumbItemInline[]
  viewMode: 'table' | 'grid'
  setViewMode: (m: 'table' | 'grid') => void
  detailVisible: boolean
  toggleDetail: () => void
  historyVisible: boolean
  toggleHistory: () => void
  setCurrentPath: (p: BreadcrumbItemInline[]) => void
  // optional callback for navigation (parent can update the URL)
  onNavigate?: (folderId?: string) => void
  onCreateFolder?: () => void
  onUploadFile?: () => void
}>

export function NavigationBar({ breadcrumb, viewMode, setViewMode, detailVisible, toggleDetail, historyVisible, toggleHistory, setCurrentPath, onNavigate, onCreateFolder, onUploadFile }: NavigationBarProps) {
  const { t } = useTranslation()

  return (
    <div className="d-flex align-items-center justify-content-between border-bottom px-3" style={{ height: 56 }}>
      <div className="d-flex align-items-center gap-2">
        {/* Root block: home icon only */}
        <button
          type="button"
          className="btn btn-sm btn-light p-1"
          style={{ borderRadius: 4, display: 'inline-flex', alignItems: 'center' }}
          onClick={() => {
            setCurrentPath([{ id: undefined, name: '/' }])
            onNavigate?.(undefined)
          }}
          aria-label="Root"
        >
          <CIcon icon={cilHome} />
        </button>

        {/* Breadcrumb blocks glued together */}
        <div className="d-flex" style={{ gap: 0 }}>
          {breadcrumb.slice(1).map((item, idx) => (
            <button
              key={`${item.name ?? 'seg'}-${idx}`}
              type="button"
              className="btn btn-sm btn-light p-1"
              style={{ borderRadius: 0, borderLeft: '1px solid rgba(0,0,0,0.06)' }}
              onClick={() => {
                // build new path up to this item
                const newPath = breadcrumb.slice(0, idx + 2)
                setCurrentPath(newPath)
                // call parent navigation with the id of this breadcrumb
                onNavigate?.(item.id)
              }}
            >
              {item.name}
            </button>
          ))}
        </div>
      </div>

      <div className="d-flex align-items-center gap-2">
        <CButton color="light" size="sm" onClick={onCreateFolder} title={t('store.createFolder')}>
          <CIcon icon={cilFolder} />
        </CButton>
        <CButton color="light" size="sm" onClick={onUploadFile} title={t('store.upload')}>
          <CIcon icon={cilCloudUpload} />
        </CButton>
        <div className="vr mx-2"></div>
        <CButton color={viewMode === 'grid' ? 'primary' : 'light'} size="sm" onClick={() => setViewMode('grid')}>
          <CIcon icon={cilGrid} />
        </CButton>
        <CButton color={viewMode === 'table' ? 'primary' : 'light'} size="sm" onClick={() => setViewMode('table')}>
          <CIcon icon={cilList} />
        </CButton>

        <CButton color={detailVisible ? 'primary' : 'light'} size="sm" onClick={toggleDetail} title={detailVisible ? 'Hide details' : 'Show details'}>
          <CIcon icon={cilInfo} />
        </CButton>
        <CButton color={historyVisible ? 'primary' : 'light'} size="sm" onClick={toggleHistory} title="Historique des versions">
          <CIcon icon={cilHistory} />
        </CButton>
      </div>
    </div>
  )
}
