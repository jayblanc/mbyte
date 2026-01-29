import { CCard, CCardBody } from '@coreui/react'
import { CIcon } from '@coreui/icons-react'
import { useEffect, useState } from 'react'
import Node from '../../api/entities/Node'
import { getIcon } from '../../utils/iconMapper'

type InfoPanelProps = Readonly<{
  selected: Node | null
}>

export function InfoPanel({ selected }: InfoPanelProps) {
  const [windowWidth, setWindowWidth] = useState(window.innerWidth)

  useEffect(() => {
    const handleResize = () => {
      setWindowWidth(window.innerWidth)
    }

    window.addEventListener('resize', handleResize)
    return () => {
      window.removeEventListener('resize', handleResize)
    }
  }, [])

  const formatSize = (size?: number) => {
    if (!size) return ''
    if (size < 1024) return `${size} B`
    if (size < 1024 * 1024) return `${Math.round(size / 1024)} KB`
    return `${Math.round(size / (1024 * 1024))} MB`
  }

  const maxLength = windowWidth < 768 ? 20 : 40
  const truncateName = (name: string) => name.length > maxLength ? name.substring(0, maxLength - 3) + '...' : name

  return (
    <div style={{ width: 360, borderLeft: '1px solid rgba(0,0,0,0.08)', overflow: 'auto' }}>
      <div className="p-3">
        <h6>Details</h6>
        {!selected && <div className="text-muted small">Select an item to see details</div>}
        {selected && (
          <CCard>
            <CCardBody>
              <div className="d-flex align-items-center gap-2 mb-2">
                <CIcon icon={getIcon(selected)} />
                {selected.isFolder ? (
                  <strong>Folder name: <span title={selected.name}>{truncateName(selected.name)}</span></strong>
                ) : (
                  <strong title={selected.name}>{truncateName(selected.name)}</strong>
                )}
              </div>
              {selected.isFolder ? (
                <>
                  <div className="text-muted small">Type: folder</div>
                  <div className="text-muted small">Modified: {selected.modificationTs ? new Date(selected.modificationTs).toLocaleString() : ''}</div>
                  {/* Future: size calculation */}
                </>
              ) : (
                <>
                  <div className="text-muted small">Type: file</div>
                  <div className="text-muted small">MIME Type: {selected.mimetype || 'Unknown'}</div>
                  <div className="text-muted small">Size: {formatSize(selected.size)}</div>
                  <div className="text-muted small">Modified: {selected.modificationTs ? new Date(selected.modificationTs).toLocaleString() : ''}</div>
                </>
              )}
            </CCardBody>
          </CCard>
        )}
      </div>
    </div>
  )
}
