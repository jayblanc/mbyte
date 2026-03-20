import { useEffect, useState, useRef, useCallback } from 'react'
import { CContainer } from '@coreui/react'
import { NavigationBar } from '../components/store/NavigationBar'
import { BrowserArea } from '../components/store/BrowserArea'
import { InfoPanel } from '../components/store/InfoPanel'
import { CreateModal } from '../components/store/CreateModal'
import { ConfirmUpdateModal } from '../components/store/ConfirmUpdateModal'
import Node from '../api/entities/Node'
import { useParams, useNavigate } from 'react-router-dom'
import { useAccessToken } from '../auth/useAccessToken'
import { useStoreApi } from '../api/useStoreApi'
import { useManagerStatus } from '../auth/useManagerStatus'
import { apiConfig } from '../api/apiConfig'
import { useProfile } from '../auth/useProfile'
import { fetchWithAuth } from '../api/fetchWithAuth'
import {
  useAutoVersioning,
  emitFileSaved,
  emitVersionRestored,
  VersionHistoryPanel,
  RestoreConfirmModal,
  FileVersion,
  type FetchFn,
} from '../versioning'

type BreadcrumbItem = { id?: string, name: string }

export function StorePage() {
  const [viewMode, setViewMode] = useState<'table' | 'grid'>('table')
  const [detailVisible, setDetailVisible] = useState(true)
  const [historyVisible, setHistoryVisible] = useState(false)
  const [currentPath, setCurrentPath] = useState<BreadcrumbItem[]>([{ id: undefined, name: '/' }])
  const [nodes, setNodes] = useState<Node[]>([])
  const [selected, setSelected] = useState<Node | null>(null)
  const [loading, setLoading] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [modalType, setModalType] = useState<'folder' | 'file'>('folder')
  const [showRestoreModal, setShowRestoreModal] = useState(false)
  const [versionToRestore, setVersionToRestore] = useState<FileVersion | null>(null)
  const [showUpdateModal, setShowUpdateModal] = useState(false)
  const [pendingFile, setPendingFile] = useState<{ file: File; parentId: string } | null>(null)

  const params = useParams()
  const navigate = useNavigate()

  const tokenProvider = useAccessToken()
  const { apps } = useManagerStatus()
  const { profile } = useProfile()

  // compute per-user store base URL from the DOCKER_STORE app name when present
  const userStoreApp = apps.find((a) => a?.type === 'DOCKER_STORE')
  const storeBaseUrl = userStoreApp?.name ? `${apiConfig.storesScheme}://${userStoreApp.name}.${apiConfig.storesDomain}/` : undefined

  // Create a stable fetchFn for the versioning module
  const storeFetchFn: FetchFn = useCallback(
    (path: string, init?: RequestInit) => fetchWithAuth(tokenProvider, path, init, storeBaseUrl),
    [tokenProvider, storeBaseUrl]
  )

  useAutoVersioning(storeFetchFn)

  const storeApi = useStoreApi(tokenProvider, storeBaseUrl)

  // Keep a ref to the current storeApi and a lastRun key to avoid repeating the same work
  const storeApiRef = useRef(storeApi)
  useEffect(() => { storeApiRef.current = storeApi }, [storeApi])
  const lastRunRef = useRef<string | null>(null)

  // react to route param (folder/file id) — single effect, deduped by key including storeBaseUrl
  useEffect(() => {
    const base = storeBaseUrl ?? ''
    if (!base) return
    if (!storeApiRef.current.isConfigured) return
    const raw = params['*'] || undefined
    const key = `${base}|${raw ?? ''}|${reloadKey}`
    if (lastRunRef.current === key) return
    lastRunRef.current = key

    let cancelled = false

    const loadRoot = async (setBreadcrumb: boolean) => {
      const root = await storeApiRef.current.getRoot()
      const coll = await storeApiRef.current.listChildren(root.id, 200, 0)
      if (cancelled) return
      const sortedValues = coll.values.sort((a, b) => {
        if (a.isFolder && !b.isFolder) return -1
        if (!a.isFolder && b.isFolder) return 1
        return a.name.localeCompare(b.name)
      })
      if (JSON.stringify(sortedValues.map(n => n.id)) !== JSON.stringify(nodes.map(n => n.id))) {
        setNodes(sortedValues)
      }
      if (setBreadcrumb) {
        const newPath = [{ id: undefined, name: '/' }]
        if (JSON.stringify(newPath) !== JSON.stringify(currentPath)) setCurrentPath(newPath)
      } else if (!cancelled && currentPath.length !== 0) {
          setCurrentPath([])
      }
      if (!cancelled && selected !== null) setSelected(null)
    }

    const run = async () => {
      setLoading(true)
      try {
        if (!raw) {
          await loadRoot(true)
          return
        }

        try {
          const node = await storeApiRef.current.getNode(raw)
          if (node.isFolder) {
            const coll = await storeApiRef.current.listChildren(node.id, 200, 0)
            if (cancelled) return
            const sortedValues = coll.values.sort((a, b) => {
              if (a.isFolder && !b.isFolder) return -1
              if (!a.isFolder && b.isFolder) return 1
              return a.name.localeCompare(b.name)
            })
            if (JSON.stringify(sortedValues.map(n => n.id)) !== JSON.stringify(nodes.map(n => n.id))) {
              setNodes(sortedValues)
            }
            try {
              const path = await storeApiRef.current.getPath(node.id)
              if (cancelled) return
              const newPath = [{ id: undefined, name: '/' }, ...path.slice(1).map(p => ({ id: p.id, name: p.name }))]
              if (JSON.stringify(newPath) !== JSON.stringify(currentPath)) setCurrentPath(newPath)
            } catch (e) {
              // eslint-disable-next-line no-console
              console.debug('Failed to fetch path for folder, hiding breadcrumb', e)
              if (!cancelled && currentPath.length !== 0) setCurrentPath([])
            }
            if (!cancelled && selected !== null) setSelected(null)
            return
          }

          if (node.isFile) {
            try {
              const path = await storeApiRef.current.getPath(node.id)
              if (cancelled) return
              const parentPath = path.slice(0, -1)
              const parentId = parentPath.length > 0 ? parentPath.at(-1)!.id : (await storeApiRef.current.getRoot()).id
              const coll = await storeApiRef.current.listChildren(parentId, 200, 0)
              if (cancelled) return
              const sortedValues = coll.values.sort((a, b) => {
                if (a.isFolder && !b.isFolder) return -1
                if (!a.isFolder && b.isFolder) return 1
                return a.name.localeCompare(b.name)
              })
              if (JSON.stringify(sortedValues.map(n => n.id)) !== JSON.stringify(nodes.map(n => n.id))) {
                setNodes(sortedValues)
              }
              const newPath = [{ id: undefined, name: '/' }, ...parentPath.slice(1).map(p => ({ id: p.id, name: p.name }))]
              if (JSON.stringify(newPath) !== JSON.stringify(currentPath)) setCurrentPath(newPath)
            } catch (e) {
              // eslint-disable-next-line no-console
              console.debug('Failed to fetch path for file, fallback to root', e)
              await loadRoot(false)
            }
            if (!cancelled && (selected?.id ?? null) !== node.id) setSelected(node)
            return
          }

          // fallback: load root
          await loadRoot(false)
        } catch (e) {
          // can't resolve id: fallback to root
          // eslint-disable-next-line no-console
          console.debug('Failed to resolve node from URL, fallback to root', e)
          await loadRoot(false)
        }
      } catch (err) {
        // Errors are reported by useStoreApi via toaster; fallback to empty state
        // eslint-disable-next-line no-console
        console.error('Failed during store load', err)
        if (!cancelled) {
          setNodes([])
          setSelected(null)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void run()
    return () => { cancelled = true }
  }, [params, storeBaseUrl, reloadKey])

  const handleOpenFolder = (folderId?: string) => {
    // update URL to point to this folder id or root
    navigate(`/s/0${folderId ? `/${folderId}` : ''}`)
  }

  const handleView = async (n?: Node | null) => {
    if (!n) return
    if (n.isFolder) {
      handleOpenFolder(n.id)
      return
    }
    // file: open preview in new tab
    if (!storeApi.isConfigured) return
    try {
      const res = await storeApi.content(n.id, false)
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      window.open(url, '_blank')
      setSelected(n)
    } catch (err) {
      console.error('Preview failed', err)
      setSelected(n)
    }
  }

  const handleAction = async (action: string, f: Node) => {
    if (!storeApi.isConfigured) return
    if (action === 'download') {
      try {
        const res = await storeApi.content(f.id, true)
        const blob = await res.blob()
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = f.name ?? 'download'
        document.body.appendChild(a)
        a.click()
        a.remove()
        URL.revokeObjectURL(url)
      } catch (err) {
        console.error('Download failed', err)
      }
    } else if (action === 'info') {
      setSelected(f)
      setDetailVisible(true)
    }
  }

  const handleCreateFolder = () => {
    setModalType('folder')
    setShowCreateModal(true)
  }

  const handleUploadFile = () => {
    setModalType('file')
    setShowCreateModal(true)
  }

  const handleModalConfirm = async (data: string | File) => {
    // Route is /s/:index/* so params['*'] is the folder/file ID (if any)
    const raw = params['*']
    const rootNode = await storeApi.getRoot()
    const parentId = raw || rootNode.id
    try {
      if (modalType === 'folder') {
        await storeApi.create(parentId, data as string)
      } else {
        const file = data as File
        const locationHeader = await storeApi.create(parentId, file.name, file)
        const nodeId = locationHeader?.split('/').pop() ?? parentId
        emitFileSaved({
          nodeId,
          nodeName: file.name,
          content: file,
          author: profile?.email ?? profile?.username ?? 'unknown',
        })
      }
      setReloadKey(k => k + 1)
      setShowCreateModal(false)
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : String(err)
      if (errorMessage.includes('already-exists') || errorMessage.includes('already exists')) {
        // File already exists on backend - offer to save a new version locally
        const file = data as File
        setPendingFile({ file, parentId })
        setShowCreateModal(false)
        setShowUpdateModal(true)
      } else {
        console.error('Create failed', err)
      }
    }
  }

  const handleConfirmUpdate = async () => {
    if (!pendingFile) return

    try {
      // Find the existing node to link the version to the same nodeId
      const existingNode = nodes.find(n => n.name === pendingFile.file.name)
      const nodeId = existingNode?.id ?? pendingFile.parentId

      // Save as a new version via the versioning API
      emitFileSaved({
        nodeId,
        nodeName: pendingFile.file.name,
        content: pendingFile.file,
        author: profile?.email ?? profile?.username ?? 'unknown',
      })
    } finally {
      setShowUpdateModal(false)
      setPendingFile(null)
    }
  }

  const handlePreviewVersion = async (version: FileVersion) => {
    try {
      const res = await storeFetchFn(`/api/versions/${encodeURIComponent(version.id)}/content`)
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      window.open(url, '_blank')
    } catch (err) {
      console.error('Preview version failed', err)
    }
  }

  const handleRestoreVersion = (version: FileVersion) => {
    setVersionToRestore(version)
    setShowRestoreModal(true)
  }

  const handleConfirmRestore = async () => {
    if (!versionToRestore || !selected) return

    try {
      // Fetch the version content from backend versioning API
      const contentRes = await storeFetchFn(`/api/versions/${encodeURIComponent(versionToRestore.id)}/content`)
      const blob = await contentRes.blob()

      // Create a new version from the restored content (tracability)
      emitFileSaved({
        nodeId: selected.id,
        nodeName: selected.name,
        content: blob,
        author: profile?.email ?? profile?.username ?? 'unknown',
      })

      emitVersionRestored({
        versionId: versionToRestore.id,
        nodeId: selected.id,
        nodeName: selected.name,
      })

      globalThis.dispatchEvent(
        new CustomEvent('mbyte-toast', {
          detail: { message: 'Fichier restauré avec succès. Une nouvelle version a été créée.' },
        })
      )

      setReloadKey(k => k + 1)
    } catch (err) {
      console.error('Restore failed', err)
    } finally {
      setShowRestoreModal(false)
      setVersionToRestore(null)
    }
  }

  if (!storeApi.isConfigured) {
    return (
      <CContainer fluid className="p-0 d-flex align-items-center justify-content-center" style={{ height: '100%' }}>
        <div style={{ maxWidth: 800, padding: 24 }}>
          <h4>Store not configured</h4>
          <p className="text-muted">The store API base URL is not configured for this environment. For local development you can set <code>VITE_API_STORE_BASE_URL</code> or provide a <code>storeLocator</code> to the API provider.</p>
        </div>
      </CContainer>
    )
  }

  return (
    <CContainer fluid className="p-0" style={{ height: '100%' }}>
      <NavigationBar
        breadcrumb={currentPath}
        viewMode={viewMode}
        setViewMode={setViewMode}
        detailVisible={detailVisible}
        toggleDetail={() => setDetailVisible((v) => !v)}
        historyVisible={historyVisible}
        toggleHistory={() => setHistoryVisible((v) => !v)}
        setCurrentPath={setCurrentPath}
        onNavigate={(folderId) => handleOpenFolder(folderId)}
        onCreateFolder={handleCreateFolder}
        onUploadFile={handleUploadFile}
      />

      <div className="d-flex" style={{ height: 'calc(100% - 56px)', overflow: 'hidden' }}>
        <div className="flex-grow-1" style={{ overflow: 'auto', minWidth: 0, position: 'relative' }}>
          {loading && (
            <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 10, background: 'rgba(255,255,255,0.6)' }}>
              <div className="spinner-border visually-hidden-focusable" aria-hidden="true" />
            </div>
          )}

          <BrowserArea files={nodes} viewMode={viewMode} onSelect={(n) => handleView(n)} onAction={(a, f) => handleAction(a, f)} showParent={currentPath.length > 1} onGoToParent={() => handleOpenFolder(currentPath[currentPath.length - 2].id)} />
        </div>

        {detailVisible && <InfoPanel selected={selected ?? null} />}
        {historyVisible && (
          <VersionHistoryPanel
            selected={selected ?? null}
            fetchFn={storeFetchFn}
            onPreview={handlePreviewVersion}
            onRestore={handleRestoreVersion}
          />
        )}
      </div>

      {showCreateModal && (
        <CreateModal
          visible={showCreateModal}
          type={modalType}
          onConfirm={handleModalConfirm}
          onClose={() => setShowCreateModal(false)}
        />
      )}

      <RestoreConfirmModal
        visible={showRestoreModal}
        version={versionToRestore}
        onConfirm={handleConfirmRestore}
        onClose={() => {
          setShowRestoreModal(false)
          setVersionToRestore(null)
        }}
      />

      <ConfirmUpdateModal
        visible={showUpdateModal}
        fileName={pendingFile?.file.name ?? ''}
        onConfirm={handleConfirmUpdate}
        onCancel={() => {
          setShowUpdateModal(false)
          setPendingFile(null)
        }}
      />
    </CContainer>
  )
}
