import { useEffect, useState } from 'react'
import { CTable, CTableHead, CTableRow, CTableHeaderCell, CTableBody, CTableDataCell, CButton } from '@coreui/react'
import { CIcon } from '@coreui/icons-react'
import { cilFolderOpen, cilCloudDownload, cilInfo } from '@coreui/icons'
import { useTranslation } from 'react-i18next'
import Node from '../../api/entities/Node'
import { getIcon } from '../../utils/iconMapper'

type BrowserAreaProps = Readonly<{
  files: Node[]
  viewMode: 'table' | 'grid'
  onSelect: (f: Node) => void
  onAction?: (action: string, file: Node) => void
  showParent?: boolean
  onGoToParent?: () => void
}>

export function BrowserArea({ files, viewMode, onSelect, onAction, showParent, onGoToParent }: BrowserAreaProps) {
  const { t } = useTranslation()
  const [windowWidth, setWindowWidth] = useState(window.innerWidth)

  useEffect(() => {
    const handleResize = () => setWindowWidth(window.innerWidth)
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  const maxLength = windowWidth < 768 ? 20 : 40
  const truncateName = (name: string) => name.length > maxLength ? name.substring(0, maxLength - 3) + '...' : name

  // fill all available space, no padding
  return (
    <div style={{ height: '100%', width: '100%', overflow: 'auto' }}>
      {viewMode === 'table' ? (
        <CTable hover responsive className="mb-0" style={{ borderRadius: 0, width: '100%', borderCollapse: 'collapse' }}>
          <CTableHead style={{ boxSizing: 'border-box' }}>
            <CTableRow style={{ height: 56, minHeight: 56, boxSizing: 'border-box' }}>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '43%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box' }}>{t('store.name')}</CTableHeaderCell>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '8%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box' }} className="text-end">{t('store.size')}</CTableHeaderCell>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '20%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box' }} className="text-end">{t('store.type')}</CTableHeaderCell>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '8%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box' }} className="text-end">{t('store.creation')}</CTableHeaderCell>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '7%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box' }} className="text-end">{t('store.modification')}</CTableHeaderCell>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '10%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box', whiteSpace: 'nowrap' }} className="text-end">{t('store.actions')}</CTableHeaderCell>
            </CTableRow>
          </CTableHead>
          <CTableBody>
            {showParent && (
              <CTableRow
                onClick={() => onGoToParent?.()}
                style={{ cursor: 'pointer', minHeight: 56, boxSizing: 'border-box' }}
              >
                <CTableDataCell style={{ background: '#fafafa', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  <span style={{ marginRight: 8 }}>
                    <CIcon icon={cilFolderOpen} />
                  </span>
                  <strong>..</strong>
                </CTableDataCell>
                <CTableDataCell style={{ background: '#fafafa', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end"></CTableDataCell>
                <CTableDataCell style={{ background: '#fafafa', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end"></CTableDataCell>
                <CTableDataCell style={{ background: '#fafafa', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end"></CTableDataCell>
                <CTableDataCell style={{ background: '#fafafa', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end"></CTableDataCell>
                <CTableDataCell style={{ background: '#fafafa', width: '10%', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box', whiteSpace: 'nowrap' }} className="text-end">
                </CTableDataCell>
              </CTableRow>
            )}
            {files.map((f, idx) => (
              <CTableRow
                key={f.id}
                onClick={() => onSelect(f)}
                style={{ cursor: 'pointer', minHeight: 56, boxSizing: 'border-box' }}
              >
                <CTableDataCell style={{ background: (idx + (showParent ? 1 : 0)) % 2 === 0 ? '#fafafa' : '#f5f6f7', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  <span style={{ marginRight: 8 }}>
                    <CIcon icon={getIcon(f)} />
                  </span>
                  {f.isFolder ? <strong title={f.name}>{truncateName(f.name)}</strong> : <span title={f.name}>{truncateName(f.name)}</span>}
                </CTableDataCell>
                <CTableDataCell style={{ background: (idx + (showParent ? 1 : 0)) % 2 === 0 ? '#fafafa' : '#f5f6f7', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end">{f.size ? `${Math.round(f.size / 1024)} KB` : ''}</CTableDataCell>
                <CTableDataCell style={{ background: (idx + (showParent ? 1 : 0)) % 2 === 0 ? '#fafafa' : '#f5f6f7', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end">{f.mimetype || 'Unknown'}</CTableDataCell>
                <CTableDataCell style={{ background: (idx + (showParent ? 1 : 0)) % 2 === 0 ? '#fafafa' : '#f5f6f7', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end">{f.creationTs ? new Date(f.creationTs).toLocaleDateString() : ''}</CTableDataCell>
                <CTableDataCell style={{ background: (idx + (showParent ? 1 : 0)) % 2 === 0 ? '#fafafa' : '#f5f6f7', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end">{f.modificationTs ? new Date(f.modificationTs).toLocaleDateString() : ''}</CTableDataCell>
                <CTableDataCell style={{ background: (idx + (showParent ? 1 : 0)) % 2 === 0 ? '#fafafa' : '#f5f6f7', width: '10%', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box', whiteSpace: 'nowrap' }} className="text-end">
                  {f.isFolder ? (
                    <div className="d-flex gap-2 justify-content-end">
                      <CButton color="light" size="sm" onClick={(e) => { e.stopPropagation(); onAction?.('info', f) }}>
                        <CIcon icon={cilInfo} />
                      </CButton>
                    </div>
                  ) : (
                    <div className="d-flex gap-2 justify-content-end">
                      <CButton color="light" size="sm" onClick={(e) => { e.stopPropagation(); onAction?.('download', f) }}>
                        <CIcon icon={cilCloudDownload} />
                      </CButton>
                      <CButton color="light" size="sm" onClick={(e) => { e.stopPropagation(); onAction?.('info', f) }}>
                        <CIcon icon={cilInfo} />
                      </CButton>
                    </div>
                  )}
                </CTableDataCell>
              </CTableRow>
             ))}
           </CTableBody>
         </CTable>
      ) : (
        <div className="row g-3 p-3">
          {showParent && (
            <div className="col-6 col-md-4 col-lg-3">
              <button type="button" className="btn p-0" onClick={() => onGoToParent?.()} style={{ cursor: 'pointer', display: 'block', textAlign: 'left', border: 'none', background: 'transparent' }}>
                <div className="card h-100">
                  <div className="card-body">
                    <div className="d-flex align-items-center gap-2">
                      <CIcon icon={cilFolderOpen} />
                      <div>
                        <div className="fw-semibold">..</div>
                        <div className="text-muted small">{t('store.folder')}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </button>
            </div>
          )}
          {files.map((f) => (
            <div key={f.id} className="col-6 col-md-4 col-lg-3">
              <button type="button" className="btn p-0" onClick={() => onSelect(f)} style={{ cursor: 'pointer', display: 'block', textAlign: 'left', border: 'none', background: 'transparent' }}>
                <div className="card h-100">
                  <div className="card-body">
                    <div className="d-flex align-items-center gap-2">
                      <CIcon icon={getIcon(f)} />
                      <div>
                        <div className="fw-semibold" title={f.name}>{truncateName(f.name)}</div>
                        <div className="text-muted small">{f.isFolder ? 'Folder' : (f.mimetype || 'Unknown')} • {f.modificationTs ? new Date(f.modificationTs).toLocaleDateString() : ''}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
