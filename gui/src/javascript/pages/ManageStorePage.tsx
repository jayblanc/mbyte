import {useEffect, useState} from 'react'
import {CContainer, CCard, CCardBody, CCardHeader, CForm, CFormInput, CButton, CRow, CCol, CBadge} from '@coreui/react'
import {useAccessToken} from '../auth/useAccessToken'
import {useStoreApi} from '../api/useStoreApi'
import {useManagerStatus} from '../auth/useManagerStatus'
import {apiConfig} from '../api/apiConfig'

export function ManageStorePage() {
    const tokenProvider = useAccessToken()
    const {apps} = useManagerStatus()
    const [storeName, setStoreName] = useState('')
    const [status, setStatus] = useState<'online' | 'offline'>('offline')
    const [metrics, setMetrics] = useState<any>({
        system: {
            cpuLoad: 0,
            totalMemory: 0,
            freeMemory: 0,
            storeUsedBytes: 0,
            storeTotalBytes: 0,
            storeFreeBytes: 0
        },
        metrics: {},
        latestMetrics: {}
    })
    const [metricsLoading, setMetricsLoading] = useState(false)
    const userStoreApp = apps.find((a) => a?.type === 'DOCKER_STORE')
    const storeBaseUrl = userStoreApp?.name
        ? `${apiConfig.storesScheme}://${userStoreApp.name}.${apiConfig.storesDomain}/`
        : undefined
    const storeApi = useStoreApi(tokenProvider, storeBaseUrl)
    const [stats, setStats] = useState({
        size: 0,
        files: 0,
        folders: 0,
        loading: false,
    })
    const loadMetrics = async () => {
        if (!storeApi.isConfigured || !userStoreApp?.name) return;

        try {
            setMetricsLoading(true);

            const containerName = `mbyte.fr.nath.store`;
            const resManager = await fetch(
                `${apiConfig.managerBaseUrl}/api/metrics/${containerName}`,
                { headers: { Authorization: `Bearer ${await tokenProvider()}` } }
            );
            const managerData = await resManager.json();

            // 🔹 Quotas depuis le store
            const resStore = await fetch(`${storeBaseUrl}api/metrics`, {
                headers: { Authorization: `Bearer ${await tokenProvider()}` }
            });
            const storeData = await resStore.json();

            // 🔹 Fusion des données : quotas du store, utilisation du manager
            setMetrics({
                system: {
                    cpuLoad: managerData.system?.cpuLoad ?? 0,
                    totalMemory: storeData.totalMemory ?? 0,
                    freeMemory: (storeData.totalMemory ?? 0) - (managerData.system?.memoryUsed ?? 0),
                    storeUsedBytes: managerData.system?.storeUsedBytes ?? 0,
                    storeTotalBytes: storeData.storeTotalBytes ?? 0,
                    storeFreeBytes:
                        (storeData.storeTotalBytes ?? 0) -
                        (managerData.system?.storeUsedBytes ?? 0),
                },
                metrics: managerData.metrics ?? {},
                latestMetrics: managerData.latestMetrics ?? {},
            });
        } catch (e) {
            console.error("Failed to load metrics", e);
        } finally {
            setMetricsLoading(false);
        }
    };
    const computeStats = async () => {
        if (!storeApi.isConfigured) return
        setStats(s => ({...s, loading: true}))
        let totalSize = 0
        let filesCount = 0
        let foldersCount = 0
        const walk = async (parentId: string) => {
            const coll = await storeApi.listChildren(parentId, 200, 0)

            for (const n of coll.values) {
                if (n.isFolder) {
                    foldersCount++
                    await walk(n.id)
                } else {
                    filesCount++
                    totalSize += n.size ?? 0
                }
            }
        }
        try {
            const root = await storeApi.getRoot()
            await walk(root.id)

            setStats({
                size: totalSize,
                files: filesCount,
                folders: foldersCount,
                loading: false,
            })
        } catch (e) {
            console.error('Failed to compute stats', e)
            setStats(s => ({...s, loading: false}))
        }
    }
    useEffect(() => {
        const check = async () => {
            if (!storeApi.isConfigured) return;
            try {
                computeStats();
                await storeApi.getRoot();
                setStatus('online');
                setStoreName(userStoreApp?.name ?? '');
                await loadMetrics();
            } catch (e) {
                console.error('Store unreachable', e);
                setStatus('offline');
            }
        };
        check();
    }, [storeApi]);

    useEffect(() => {
        if (!storeApi.isConfigured) return
        loadMetrics() // 1er appel
        const interval = setInterval(() => {
            loadMetrics()
        }, 5000)
        return () => clearInterval(interval)
    }, [storeApi])
    const handleSave = async () => {
        console.log('Saving config:', storeName)
    }
    if (!storeApi.isConfigured) {
        return (
            <CContainer className="p-4">
                <h4>Store non configuré</h4>
                <p className="text-muted">Aucun store disponible pour cet utilisateur.</p>
            </CContainer>
        )
    }
    const handleIncreaseStorage = async () => {
        if (!storeBaseUrl) return
        try {
            const res = await fetch(`${storeBaseUrl}api/metrics`, {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${await tokenProvider()}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ maxStorage: 536870912 })
            })
            if (!res.ok) throw new Error("Failed to increase storage quota")
            const updatedMetrics = await res.json()
            console.log("Updated quotas:", updatedMetrics)
            await loadMetrics()
        } catch (e) {
            console.error(e)
        }
    }
    const handleIncreaseMemory = async () => {
        if (!storeBaseUrl) return
        try {
            const res = await fetch(`${storeBaseUrl}api/metrics`, {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${await tokenProvider()}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ maxMemory: 268435456 })
            })
            if (!res.ok) throw new Error("Failed to increase memory quota")
            const updatedMetrics = await res.json()
            console.log("Updated quotas:", updatedMetrics)
            await loadMetrics()
        } catch (e) {
            console.error(e)
        }
    }
    const formatBytes = (bytes: number) => {
        if (!bytes) return '0 B'
        const k = 1024
        const sizes = ['B', 'KB', 'MB', 'GB']
        const i = Math.floor(Math.log(bytes) / Math.log(k))
        return (bytes / Math.pow(k, i)).toFixed(1) + ' ' + sizes[i]
    }
    const percent = (value: number) => Math.round(value * 100)
    return (
        <CContainer fluid className="p-4">
            <CRow className="mb-4">
                <CCol>
                    <h3>Paramètres du Store</h3>
                </CCol>
                <CCol className="text-end">
                    <CBadge color={status === 'online' ? 'success' : 'danger'}>
                        {status === 'online' ? 'En ligne' : 'Hors ligne'}
                    </CBadge>
                </CCol>
            </CRow>
            <CCard className="mb-4">
                <CCardHeader>Configuration générale</CCardHeader>
                <CCardBody>
                    <CForm>
                        <CFormInput
                            label="Nom du store"
                            value={storeName}
                            onChange={(e) => setStoreName(e.target.value)}
                            className="mb-3"
                        />
                        <div className="d-flex gap-2">
                            <CButton color="primary" onClick={handleSave}>
                                Enregistrer
                            </CButton>
                        </div>
                    </CForm>
                </CCardBody>
            </CCard>
            <CCard className="mb-4">
                <CCardHeader>Informations</CCardHeader>
                <CCardBody>
                    <p><strong>URL :</strong> {storeBaseUrl}</p>
                    <p><strong>Type :</strong> DOCKER_STORE</p>
                    <p><strong>Status :</strong> {status}</p>
                </CCardBody>
            </CCard>
            <CCard className="mb-4">
                <CCardHeader>Informations</CCardHeader>
                <CCardBody>
                    <p><strong>URL :</strong> {storeBaseUrl}</p>
                    <p><strong>Status :</strong> {status}</p>
                    <hr/>
                    <p>
                        <strong>Fichiers :</strong>{' '}
                        {stats.loading ? '...' : stats.files}
                    </p>
                    <p>
                        <strong>Dossiers :</strong>{' '}
                        {stats.loading ? '...' : stats.folders}
                    </p>
                    <CButton
                        size="sm"
                        color="secondary"
                        onClick={computeStats}
                        disabled={stats.loading}
                    >
                        Recalculer
                    </CButton>
                </CCardBody>
            </CCard>
            <CCard className="mb-4">
                <CCardHeader>Ressources système</CCardHeader>
                <CCardBody>
                    {metricsLoading && <p>Chargement...</p>}
                    {metrics && (
                        <>
                            {/* CPU */}
                            <div className="mb-3">
                                <strong>CPU :</strong> {percent(metrics.system.cpuLoad)}%
                                <div className="progress mt-1">
                                    <div
                                        className="progress-bar"
                                        style={{ width: `${percent(metrics?.system?.cpuLoad ?? 0)}%` }}
                                    />
                                </div>
                            </div>
                            {/* Mémoire */}
                            <div className="mb-3">
                                <strong>Mémoire :</strong>{' '}
                                {formatBytes(metrics.system.totalMemory - metrics.system.freeMemory)} /{' '}
                                {formatBytes(metrics.system.totalMemory)}

                                <div className="progress mt-1">
                                    <div
                                        className="progress-bar bg-info"
                                        style={{
                                            width: `${percent(
                                                (metrics.system.totalMemory - metrics.system.freeMemory) /
                                                (metrics.system.totalMemory || 1)
                                            )}%`
                                        }}
                                    />
                                </div>
                                <div className="mt-2">
                                    <CButton
                                        size="sm"
                                        color="primary"
                                        variant="outline"
                                        onClick={handleIncreaseMemory}
                                    >
                                        + Augmenter la mémoire
                                    </CButton>
                                </div>
                            </div>
                            <div className="mb-3">
                                <strong>Stockage du store :</strong>{' '}
                                {formatBytes(metrics.system.storeUsedBytes)} /{' '}
                                {formatBytes(metrics.system.storeTotalBytes)}
                                <div className="progress mt-1">
                                    <div
                                        className="progress-bar bg-warning"
                                        style={{
                                            width: `${percent(
                                                metrics.system.storeTotalBytes > 0
                                                    ? metrics.system.storeUsedBytes / metrics.system.storeTotalBytes
                                                    : 0
                                            )}%`
                                        }}
                                    />
                                </div>
                                {/* 🔹 bouton */}
                                <div className="mt-2">
                                    <CButton
                                        size="sm"
                                        color="warning"
                                        variant="outline"
                                        onClick={handleIncreaseStorage}
                                    >
                                        + Augmenter le stockage
                                    </CButton>
                                </div>
                            </div>
                            <div className="mt-4">
                                <strong>Activité récente</strong>
                                <ul className="mt-2">
                                    {Object.entries(metrics.latestMetrics || {}).map(([k, v]) => (
                                        <li key={k}>
                                            {k}: {v as number}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </>
                    )}

                    <CButton
                        size="sm"
                        color="secondary"
                        onClick={loadMetrics}
                        className="mt-3"
                    >
                        Rafraîchir
                    </CButton>
                </CCardBody>
            </CCard>
        </CContainer>
    )
}