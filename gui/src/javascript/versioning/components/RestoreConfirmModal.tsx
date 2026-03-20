import {
  CModal,
  CModalHeader,
  CModalTitle,
  CModalBody,
  CModalFooter,
  CButton,
} from '@coreui/react'
import { CIcon } from '@coreui/icons-react'
import { cilWarning } from '@coreui/icons'
import FileVersion from '../entities/FileVersion'

type RestoreConfirmModalProps = Readonly<{
  visible: boolean
  version: FileVersion | null
  onConfirm: () => void
  onClose: () => void
}>

export function RestoreConfirmModal({
  visible,
  version,
  onConfirm,
  onClose,
}: RestoreConfirmModalProps) {
  if (!version) return null

  return (
    <CModal visible={visible} onClose={onClose} alignment="center">
      <CModalHeader>
        <CModalTitle>Restaurer une version</CModalTitle>
      </CModalHeader>
      <CModalBody>
        <div className="d-flex align-items-start gap-3">
          <CIcon icon={cilWarning} size="xl" className="text-warning" />
          <div>
            <p className="mb-2">Êtes-vous sûr de vouloir restaurer cette version ?</p>
            <div className="text-muted small">
              <div><strong>Fichier :</strong> {version.nodeName}</div>
              <div><strong>Date :</strong> {version.formattedDate}</div>
              <div><strong>Auteur :</strong> {version.author}</div>
            </div>
            <p className="mt-3 mb-0 text-muted small">
              Le fichier actuel sera remplacé par cette version. Une nouvelle version sera créée pour tracer cette action.
            </p>
          </div>
        </div>
      </CModalBody>
      <CModalFooter>
        <CButton color="secondary" onClick={onClose}>
          Annuler
        </CButton>
        <CButton color="primary" onClick={onConfirm}>
          Restaurer
        </CButton>
      </CModalFooter>
    </CModal>
  )
}
