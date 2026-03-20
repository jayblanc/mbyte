import { CModal, CModalHeader, CModalTitle, CModalBody, CModalFooter, CButton } from '@coreui/react'

type ConfirmUpdateModalProps = {
  visible: boolean
  fileName: string
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmUpdateModal({ visible, fileName, onConfirm, onCancel }: ConfirmUpdateModalProps) {
  return (
    <CModal visible={visible} onClose={onCancel}>
      <CModalHeader>
        <CModalTitle>Fichier existant</CModalTitle>
      </CModalHeader>
      <CModalBody>
        <p>Le fichier "{fileName}" existe déjà sur le serveur. Voulez-vous sauvegarder une nouvelle version ?</p>
        <p className="text-muted small">Le contenu sera enregistré comme nouvelle version dans l'historique.</p>
      </CModalBody>
      <CModalFooter>
        <CButton color="secondary" onClick={onCancel}>
          Annuler
        </CButton>
        <CButton color="primary" onClick={onConfirm}>
          Sauvegarder la version
        </CButton>
      </CModalFooter>
    </CModal>
  )
}
