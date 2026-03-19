import { CButton, CCard, CCardBody, CCardHeader, CBadge, CSpinner, CTooltip } from '@coreui/react'
import { CIcon } from '@coreui/icons-react'
import { cilReload } from '@coreui/icons'
import { useTranslation } from 'react-i18next'
import { useEffect, useState } from 'react'
import type { Application } from '../../api/entities/Application'
import type { CommandDescriptor } from '../../api/entities/CommandDescriptor'
import { formatTimeAgo } from '../../utils/time'
import { useAppCommandProcessing } from '../../app/useAppCommandProcessing'
import { useManagerApi } from '../../api/ManagerApiProvider'

export type DetailStoreCardProps = Readonly<{
  app: Application
  onRefresh: () => void
  onDeleted?: () => void
}>

export function DetailStoreCard({ app, onRefresh, onDeleted }: DetailStoreCardProps) {
  const { t } = useTranslation()
  const commandProcessing = useAppCommandProcessing(app.id)
  const managerApi = useManagerApi()

  const [commands, setCommands] = useState<CommandDescriptor[] | null>(null)
  const [commandsError, setCommandsError] = useState<string | null>(null)
  const [commandsLoading, setCommandsLoading] = useState(false)
  const [deleteBusy, setDeleteBusy] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setCommandsLoading(true)
    setCommandsError(null)
    managerApi
      .listAppCommands(app.id)
      .then((res) => {
        if (cancelled) return
        setCommands(res)
      })
      .catch((err) => {
        if (cancelled) return
        console.error('Failed to load commands:', err)
        setCommandsError(err instanceof Error ? err.message : String(err))
      })
      .finally(() => {
        if (cancelled) return
        setCommandsLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [app.id, managerApi])

  useEffect(() => {
    if (commandProcessing.phase === 'completed') {
      // refresh parent state (app status) when a process completes
      onRefresh()
    }
  }, [commandProcessing.phase, onRefresh])

  const getStatusColor = (status: string | undefined) => {
    switch (status) {
      case 'AVAILABLE':
        return 'success'
      case 'STARTED':
        return 'success'
      case 'STARTING':
        return 'warning'
      case 'CREATED':
        return 'info'
      case 'STOPPED':
        return 'secondary'
      case 'ERROR':
        return 'danger'
      case 'LOST':
        return 'secondary'
      default:
        return 'primary'
    }
  }

  const createdSince = app.creationDate ? formatTimeAgo(new Date(app.creationDate)) : t('common.unknown')

  const handleCommand = (commandName: string) => {
    void commandProcessing.runCommand(app.id, commandName)
  }

  const handleDeleteStore = async () => {
    try {
      setDeleteBusy(true)
      setDeleteError(null)
      await managerApi.deleteApp(app.id)
      onDeleted?.()
    } catch (err) {
      console.error('Failed to delete store:', err)
      setDeleteError(err instanceof Error ? err.message : String(err))
    } finally {
      setDeleteBusy(false)
    }
  }

  return (
    <CCard>
      <CCardHeader className="d-flex justify-content-between align-items-center">
        <div className="d-flex align-items-center gap-2">
          <strong>'{app.name || t('common.unknown')}' {t('dashboard.storeDetail.title')}</strong>
          <CBadge color={getStatusColor(app.status)}>{app.status}</CBadge>
        </div>
        <CButton
          color="light"
          size="sm"
          onClick={onRefresh}
          disabled={deleteBusy}
          title={t('dashboard.storeDetail.refresh')}
        >
          <CIcon icon={cilReload} />
        </CButton>
      </CCardHeader>
      <CCardBody>
        <div className="mb-3">
          <p className="text-muted small mb-2">{t('dashboard.storeDetail.id')}: {app.id}</p>
          {app.creationDate && (
            <div className="text-muted small">
              {t('dashboard.storeDetail.createdSince')}: {createdSince}
            </div>
          )}
        </div>

        {commandProcessing.phase === 'running' && (
          <div className="mb-3 d-flex align-items-center gap-2">
            <CSpinner size="sm" />
            <span>{t('dashboard.storeDetail.starting')}</span>
          </div>
        )}

        {commandProcessing.phase === 'polling' && (
          <div className="mb-3 d-flex align-items-center gap-2">
            <CSpinner size="sm" />
            <span>{t('dashboard.storeDetail.running')}</span>
          </div>
        )}

        {commandProcessing.phase === 'completed' && commandProcessing.currentProcess && (
          <div className="mb-3">
            <CBadge color={commandProcessing.currentProcess.status === 'COMPLETED' ? 'success' : 'danger'}>
              {commandProcessing.currentProcess.status}
            </CBadge>
          </div>
        )}

        {commandProcessing.error && (
          <div className="mb-3 text-danger">
            {commandProcessing.error}
          </div>
        )}

        {deleteError && (
          <div className="mb-3 text-danger">
            {deleteError}
          </div>
        )}

        <div>
          {commandsLoading && (
            <div className="d-flex align-items-center gap-2">
              <CSpinner size="sm" />
              <span>{t('dashboard.storeDetail.loadingCommands')}</span>
            </div>
          )}

          {commandsError && (
            <div className="text-danger">{commandsError}</div>
          )}

          {commands?.length === 0 && (
            <div className="text-muted small">{t('dashboard.storeDetail.noCommands')}</div>
          )}

          {commands?.length ? (
            <div className="d-flex flex-wrap gap-2 align-items-center">
              {commands.map((cmd) => {
                const appStatus = app.status ?? ''
                const allowedForStatus = !cmd.appStatus || cmd.appStatus.includes(appStatus)
                const disabled = deleteBusy || commandProcessing.phase !== 'idle' || !allowedForStatus
                const btnStyle = allowedForStatus ? undefined : { opacity: 0.5 }
                return (
                  cmd.description ? (
                    <CTooltip key={cmd.name} content={t(`commands.${cmd.name}.description`)}>
                      <span>
                        <CButton
                          color="outline-primary"
                          size="sm"
                          onClick={() => handleCommand(cmd.name)}
                          disabled={disabled}
                          aria-disabled={disabled}
                          style={btnStyle}
                        >
                          {t(`commands.${cmd.name}.label`)}
                        </CButton>
                      </span>
                    </CTooltip>
                  ) : (
                    <CButton
                      key={cmd.name}
                      color="outline-primary"
                      size="sm"
                      title={t(`commands.${cmd.name}.description`)}
                      onClick={() => handleCommand(cmd.name)}
                      disabled={disabled}
                      aria-disabled={disabled}
                      style={btnStyle}
                    >
                      {t(`commands.${cmd.name}.label`)}
                    </CButton>
                  )
                )
              })}
              <CButton
                color="outline-danger"
                size="sm"
                onClick={handleDeleteStore}
                disabled={deleteBusy || commandProcessing.phase !== 'idle'}
              >
                {deleteBusy ? t('dashboard.storeDetail.deleting') : t('dashboard.storeDetail.delete')}
              </CButton>
            </div>
          ) : null}
        </div>
      </CCardBody>
    </CCard>
  )
}
