import { useState } from 'react';
import {
    CModal,
    CModalHeader,
    CModalTitle,
    CModalBody,
    CModalFooter,
    CButton,
    CForm,
    CFormLabel,
    CFormInput,
    CFormCheck,
    CRow,
    CCol,
    CAlert
} from '@coreui/react';
import {apiConfig} from "../../api/apiConfig.ts";

interface AddWebHookModalProps {
    visible: boolean;
    setVisible: (visible: boolean) => void;
    owner: string | undefined;
    onSuccess: () => void
}

const AddWebHookModal = ({ visible, setVisible, owner, onSuccess}: AddWebHookModalProps) => {
    const [url, setUrl] = useState('');
    const [selectedEvents, setSelectedEvents] = useState<string[]>([]);
    const [error, setError] = useState<string | null>(null);

    const availableEvents = [
        { id: 'FILE_UPLOAD', label: 'Ajout de fichier' },
        { id: 'FILE_DELETE', label: 'Suppression de fichier' },
        { id: 'FILE_UPDATE', label: 'Mise à jour de fichier' },
        { id: 'DIRECTORY_CREATE', label: 'Création de dossier' },
        { id: 'DIRECTORY_DELETE', label: 'Suppression de dossier' },
        { id: 'FILE_DOWNLOAD', label: 'Téléchargement de fichier' },
        { id: 'FILE_SHARED', label: 'Accès à un dossier partagé' }
    ];

    const handleCheck = (eventId: string) => {
        setSelectedEvents(prev =>
            prev.includes(eventId) ? prev.filter(id => id !== eventId) : [...prev, eventId]
        );
    };

    const handleSubmit = async () => {
        if (!url) {
            setError("Veuillez entrer une URL valide.");
            return;
        }
        if (selectedEvents.length === 0) {
            setError("Sélectionnez au moins un événement.");
            return;
        }
        if (!owner){
            setError("Problème avec l'utilisateur courant");
            return;
        }

        try {
            const formData = new URLSearchParams();
            formData.append('owner', owner);
            formData.append('url', url);
            selectedEvents.forEach(event => formData.append('events', event));

            const response = await fetch(apiConfig.managerBaseUrl + '/api/webhooks', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData
            });

            if (response.ok) {
                onSuccess();
                setUrl('');
                setSelectedEvents([]);
            } else {
                setError("Une erreur est survenue lors de l'enregistrement.");
            }
        } catch (err) {
            setError("Impossible de contacter le serveur.");
        }
    };

    return (
        <CModal visible={visible} onClose={() => setVisible(false)} size="lg">
            <CModalHeader>
                <CModalTitle>Ajouter un Webhook</CModalTitle>
            </CModalHeader>
            <CModalBody>
                <p className="text-muted">Configurez une URL de callback pour recevoir des notifications d'événements.</p>

                {error && <CAlert color="danger">{error}</CAlert>}

                <CForm>
                    <div className="mb-3">
                        <CFormLabel>URL du Webhook :</CFormLabel>
                        <CFormInput
                            type="url"
                            placeholder="https://www.exemple.com/webhook"
                            value={url}
                            onChange={(e) => setUrl(e.target.value)}
                        />
                    </div>

                    <div className="mb-3">
                        <CFormLabel>Secret (optionnel) :</CFormLabel>
                        <CFormInput type="text" disabled placeholder="Généré automatiquement par le système" />
                    </div>

                    <CFormLabel>Événements à surveiller :</CFormLabel>
                    <CRow className="mt-2">
                        {availableEvents.map((event) => (
                            <CCol md={6} key={event.id} className="mb-2">
                                <CFormCheck
                                    id={event.id}
                                    label={event.label}
                                    checked={selectedEvents.includes(event.id)}
                                    onChange={() => handleCheck(event.id)}
                                />
                            </CCol>
                        ))}
                    </CRow>
                </CForm>
            </CModalBody>
            <CModalFooter>
                <CButton color="secondary" variant="outline" onClick={() => setVisible(false)}>
                    Annuler
                </CButton>
                <CButton color="primary" onClick={handleSubmit}>
                    Enregistrer
                </CButton>
            </CModalFooter>
        </CModal>
    );
};

export default AddWebHookModal;