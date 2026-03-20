import { useEffect, useState, useCallback } from 'react'
import {
  CCard,
  CCardBody,
  CListGroup,
  CListGroupItem,
  CButton,
  CSpinner,
  CFormSelect,
} from '@coreui/react'
import { CIcon } from '@coreui/icons-react'
import { cilHistory, cilCloudDownload, cilReload } from '@coreui/icons'
import Node from '../../api/entities/Node'
import FileVersion from '../entities/FileVersion'
import { useVersioningService } from '../useVersioningService'
import { onVersionCreated } from '../versioningEvents'
import type { FetchFn } from '../versioningService'

type SortBy = 'date-desc' | 'date-asc' | 'author'

type VersionHistoryPanelProps = Readonly<{
  selected: Node | null
  fetchFn: FetchFn
  onPreview: (version: FileVersion) => void
  onRestore: (version: FileVersion) => void
}>

export function VersionHistoryPanel({ selected, fetchFn, onPreview, onRestore }: VersionHistoryPanelProps) {
  const versioningService = useVersioningService(fetchFn)

  const [versions, setVersions] = useState<FileVersion[]>([])
  const [loading, setLoading] = useState(false)
  const [sortBy, setSortBy] = useState<SortBy>('date-desc')

  const loadVersions = useCallback(async () => {
    if (!selected || !selected.isFile) {
      setVersions([])
      return
    }

    setLoading(true)
    try {
      const history = await versioningService.getVersionHistory(selected.id)
      setVersions(history)
    } catch (err) {
      console.error('Failed to load version history', err)
      setVersions([])
    } finally {
      setLoading(false)
    }
  }, [selected, versioningService])

  useEffect(() => {
    loadVersions()
  }, [loadVersions])

  useEffect(() => {
    const unsubscribe = onVersionCreated((detail) => {
      if (selected && detail.nodeId === selected.id) {
        loadVersions()
      }
    })
    return unsubscribe
  }, [selected, loadVersions])

  const sortedVersions = [...versions].sort((a, b) => {
    switch (sortBy) {
      case 'date-asc':
        return a.timestamp - b.timestamp
      case 'author':
        return a.author.localeCompare(b.author)
      case 'date-desc':
      default:
        return b.timestamp - a.timestamp
    }
  })

  const formatDate = (ts: number) => {
    const date = new Date(ts)
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }

  return (
    <div style={{ width: 360, borderLeft: '1px solid rgba(0,0,0,0.08)', overflow: 'auto' }}>
      <div className="p-3">
        <div className="d-flex align-items-center gap-2 mb-3">
          <CIcon icon={cilHistory} />
          <h6 className="mb-0">Historique des versions</h6>
        </div>

        {!selected && (
          <div className="text-muted small">Sélectionnez un fichier pour voir son historique</div>
        )}

        {selected && !selected.isFile && (
          <div className="text-muted small">L'historique est disponible uniquement pour les fichiers</div>
        )}

        {selected && selected.isFile && (
          <>
            <div className="mb-3">
              <CFormSelect
                size="sm"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as SortBy)}
              >
                <option value="date-desc">Date (plus récent)</option>
                <option value="date-asc">Date (plus ancien)</option>
                <option value="author">Auteur</option>
              </CFormSelect>
            </div>

            {loading && (
              <div className="text-center py-3">
                <CSpinner size="sm" />
              </div>
            )}

            {!loading && versions.length === 0 && (
              <div className="text-muted small">Aucune version enregistrée</div>
            )}

            {!loading && versions.length > 0 && (
              <CCard>
                <CCardBody className="p-0">
                  <CListGroup flush>
                    {sortedVersions.map((version, index) => (
                      <CListGroupItem key={version.id} className="px-3 py-2">
                        <div className="d-flex justify-content-between align-items-start">
                          <div className="flex-grow-1">
                            <div className="fw-semibold small">
                              #{versions.length - index}
                            </div>
                            <div className="text-muted small">
                              {formatDate(version.timestamp)}
                            </div>
                            <div className="text-muted small">
                              {version.author}
                            </div>
                            {version.comment && (
                              <div className="text-muted small fst-italic">
                                {version.comment}
                              </div>
                            )}
                          </div>
                          <div className="d-flex gap-1">
                            <CButton
                              color="light"
                              size="sm"
                              title="Prévisualiser"
                              onClick={() => onPreview(version)}
                            >
                              <CIcon icon={cilCloudDownload} size="sm" />
                            </CButton>
                            <CButton
                              color="primary"
                              size="sm"
                              title="Restaurer"
                              onClick={() => onRestore(version)}
                            >
                              <CIcon icon={cilReload} size="sm" />
                            </CButton>
                          </div>
                        </div>
                      </CListGroupItem>
                    ))}
                  </CListGroup>
                </CCardBody>
              </CCard>
            )}
          </>
        )}
      </div>
    </div>
  )
}
