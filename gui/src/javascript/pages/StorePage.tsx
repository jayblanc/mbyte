import { useEffect, useState, useRef, useCallback } from 'react'
import { CContainer } from '@coreui/react'
import { NavigationBar } from '../components/store/NavigationBar'
import { BrowserArea } from '../components/store/BrowserArea'
import { InfoPanel } from '../components/store/InfoPanel'
import { CreateModal } from '../components/store/CreateModal'
import { StoreAiChat } from '../components/store/StoreAiChat'
import Node from '../api/entities/Node'
import { useParams, useNavigate } from 'react-router-dom'
import { useAccessToken } from '../auth/useAccessToken'
import { useStoreApi } from '../api/useStoreApi'
import { useManagerStatus } from '../auth/useManagerStatus'
import { apiConfig } from '../api/apiConfig'
import { selectPreferredStoreApp } from '../utils/storeApp'

type BreadcrumbItem = { id?: string, name: string }

export function StorePage() {
  const [viewMode, setViewMode] = useState<'table' | 'grid'>('table')
  const [detailVisible, setDetailVisible] = useState(true)
  const [currentPath, setCurrentPath] = useState<BreadcrumbItem[]>([{ id: undefined, name: '/' }])
  const [nodes, setNodes] = useState<Node[]>([])
  const [selected, setSelected] = useState<Node | null>(null)
  const [loading, setLoading] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [modalType, setModalType] = useState<'folder' | 'file'>('folder')

  const params = useParams()
  const navigate = useNavigate()

  const tokenProvider = useAccessToken()
  const { apps } = useManagerStatus()

  // compute per-user store base URL from the DOCKER_STORE app name when present
  const userStoreApp = selectPreferredStoreApp(apps)
  const storeBaseUrl = userStoreApp?.name ? `${apiConfig.storesScheme}://${userStoreApp.name}.${apiConfig.storesDomain}/` : undefined

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

  const handleSearch = useCallback((query: string) => storeApi.search(query), [storeApi])
  const handleSearchSelect = useCallback((id: string) => navigate(`/s/0/${id}`), [navigate])
  const handleConversationStream = useCallback(
    (
      query: string,
      conversationId: string | null,
      onChunk: (chunk: string) => void,
      onConversationId?: (id: string) => void,
    ) => storeApi.streamConversation(query, conversationId, onChunk, onConversationId),
    [storeApi],
  )

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
    const routeId = params['*']
    const currentFolderId = currentPath.at(-1)?.id
    const parentId =
      routeId && routeId === currentFolderId
        ? routeId
        : currentFolderId ?? (await storeApi.getRoot()).id
    try {
      if (modalType === 'folder') {
        await storeApi.create(parentId, data as string)
      } else {
        await storeApi.create(parentId, (data as File).name, data as File)
      }
      setReloadKey(k => k + 1)
      setShowCreateModal(false)
    } catch (err) {
      console.error('Create failed', err)
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
        setCurrentPath={setCurrentPath}
        onNavigate={(folderId) => handleOpenFolder(folderId)}
        onCreateFolder={handleCreateFolder}
        onUploadFile={handleUploadFile}
        onSearch={handleSearch}
        onSearchSelect={handleSearchSelect}
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
      </div>

      {showCreateModal && (
        <CreateModal
          visible={showCreateModal}
          type={modalType}
          onConfirm={handleModalConfirm}
          onClose={() => setShowCreateModal(false)}
        />
      )}

      <StoreAiChat streamConversation={handleConversationStream} />
    </CContainer>
  )
}
