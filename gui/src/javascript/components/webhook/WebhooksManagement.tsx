import {useState, useEffect, useCallback, useMemo} from 'react';
import {
    CCard,
    CCardBody,
    CCardHeader,
    CButton,
    CTable,
    CTableHead,
    CTableRow,
    CTableHeaderCell,
    CTableBody,
    CTableDataCell,
    CFormSwitch,
    CFormInput,
    CInputGroup,
    CSpinner
} from '@coreui/react';
import CIcon from '@coreui/icons-react';
import {cilSearch, cilPencil, cilTrash, cilLockLocked, cilPlus} from '@coreui/icons';
import AddWebHookModal from "./AddWebhookModal.tsx";
import {apiConfig} from "../../api/apiConfig.ts";
import type {Profile} from "../../api/entities/Profile.ts";
import {useAccessToken} from "../../auth/useAccessToken.ts";
import {createManagerApi} from "../../api/managerApi.ts";

// Mapping technical Enums from Quarkus entity to readable labels
const eventLabels: Record<string, string> = {
    FILE_UPLOAD: "Ajout de fichier",
    FILE_DELETE: "Suppression de fichier",
    FILE_UPDATE: "Mise à jour de fichier",
    DIRECTORY_CREATE: "Création de dossier",
    DIRECTORY_DELETE: "Suppression de dossier",
    FILE_DOWNLOAD: "Téléchargement de fichier",
    FILE_SHARED: "Accès à un dossier partagé"
};

const WebhooksPage = () => {
    const [webhooks, setWebhooks] = useState<Webhook[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [searchTerm, setSearchTerm] = useState<string>('');
    const [visible, setVisible] = useState<boolean>(false);
    const [user, setUser] = useState<Profile>()
    const defaultTokenProvider = useAccessToken()
    const apiManager = useMemo(() => createManagerApi(defaultTokenProvider), [defaultTokenProvider]);

    useEffect(() => {
        apiManager.getCurrentProfile().then((user) => setUser(user))
    }, [apiManager]);


    const fetchWebhooks = useCallback(async () => {
        setLoading(true);
        try {
            const response = await fetch(apiConfig.managerBaseUrl + `/api/webhooks?owner=${user?.id}`);
            const data = await response.json();
            setWebhooks(data);
        } catch (error) {
            console.error("Error fetching webhooks:", error);
        } finally {
            setLoading(false);
        }
    }, [user?.id]);

    useEffect(() => {
        fetchWebhooks();
    }, [fetchWebhooks]);

    const handleToggleActive = async (webhook: Webhook) => {
        const updatedStatus = !webhook.active;

        setWebhooks(webhooks.map(wh =>
            wh.id === webhook.id ? { ...wh, active: updatedStatus } : wh
        ));

        try {
            const formData = new URLSearchParams();
            formData.append('url', webhook.url);
            formData.append('active', String(updatedStatus));
            webhook.events.forEach((e: string) => formData.append('events', e));

            await fetch(apiConfig.managerBaseUrl + `/api/webhooks/${webhook.id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData
            });
        } catch (error) {
            console.error("Failed to update webhook:", error);
            fetchWebhooks(); // Rollback on error
        }
    };

    const handleDelete = async (id: string) => {
        if (window.confirm("Are you sure you want to delete this webhook?")) {
            try {
                await fetch(apiConfig.managerBaseUrl + `/api/webhooks/${id}`, { method: 'DELETE' });
                setWebhooks(webhooks.filter(wh => wh.id !== id));
            } catch (error) {
                console.error("Error deleting webhook:", error);
            }
        }
    };

    const filteredWebhooks = webhooks.filter(wh =>
        wh.url.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const onSuccess = () => {
        setVisible(false);
        fetchWebhooks();
    }

    return (
        <div className="p-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2>Webhooks</h2>
                    <p className="text-muted">Liste des webhooks configurés pour recevoir des notifications d'événements.</p>
                </div>
                <CButton color="primary" onClick={() => setVisible(true)} className="d-flex align-items-center">
                    <CIcon icon={cilPlus} className="me-2" /> Ajouter un Webhook
                </CButton>
                <AddWebHookModal visible={visible} setVisible={setVisible} owner={user?.id} onSuccess={onSuccess}/>
            </div>

            <CCard>
                <CCardHeader>
                    <div className="col-md-4 ms-auto">
                        <CInputGroup>
                            <CFormInput
                                placeholder="Rechercher..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                            <CButton color="light" variant="outline">
                                <CIcon icon={cilSearch} />
                            </CButton>
                        </CInputGroup>
                    </div>
                </CCardHeader>
                <CCardBody>
                    {loading ? (
                        <div className="text-center p-5"><CSpinner color="primary" /></div>
                    ) : (
                        <CTable align="middle" hover responsive>
                            <CTableHead color="light">
                                <CTableRow>
                                    <CTableHeaderCell>URL du Webhook</CTableHeaderCell>
                                    <CTableHeaderCell>Évènement surveillés</CTableHeaderCell>
                                    <CTableHeaderCell>Secret</CTableHeaderCell>
                                    <CTableHeaderCell>Statut</CTableHeaderCell>
                                    <CTableHeaderCell className="text-center">Actions</CTableHeaderCell>
                                </CTableRow>
                            </CTableHead>
                            <CTableBody>
                                {filteredWebhooks.map((wh) => (
                                    <CTableRow key={wh.id}>
                                        <CTableDataCell className="text-primary">{wh.url}</CTableDataCell>
                                        <CTableDataCell>
                                            {wh.events.map((e: string) => eventLabels[e] || e).join(', ')}
                                        </CTableDataCell>
                                        <CTableDataCell>
                                            {wh.secret ? (
                                                <div className="d-flex align-items-center">
                                                    <CIcon icon={cilLockLocked} className="me-1" size="sm" />
                                                    <span className="text-muted small">********</span>
                                                </div>
                                            ) : '-'}
                                        </CTableDataCell>
                                        <CTableDataCell>
                                            <CFormSwitch
                                                id={`switch-${wh.id}`}
                                                checked={wh.active}
                                                onChange={() => handleToggleActive(wh)}
                                            />
                                        </CTableDataCell>
                                        <CTableDataCell>
                                            <div className="d-flex justify-content-center gap-2">
                                                <CButton color="dark" size="sm" variant="outline">
                                                    <CIcon icon={cilPencil} className="me-1" /> Modifier
                                                </CButton>
                                                <CButton color="dark" size="sm" variant="outline" onClick={() => handleDelete(wh.id)}>
                                                    <CIcon icon={cilTrash} />
                                                </CButton>
                                            </div>
                                        </CTableDataCell>
                                    </CTableRow>
                                ))}
                            </CTableBody>
                        </CTable>
                    )}
                </CCardBody>
            </CCard>
        </div>
    );
};

export default WebhooksPage;