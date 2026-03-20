
export { default as FileVersion, type FileVersionData } from './entities/FileVersion'
export { createVersioningService, type VersioningService, type FetchFn } from './versioningService'
export { useVersioningService } from './useVersioningService'
export { useAutoVersioning } from './useAutoVersioning'
export {
  VERSIONING_EVENTS,
  emitFileSaved,
  emitVersionCreated,
  emitVersionRestored,
  onFileSaved,
  onVersionCreated,
  onVersionRestored,
  type FileSavedEventDetail,
  type VersionCreatedEventDetail,
  type VersionRestoredEventDetail,
} from './versioningEvents'
export { VersionHistoryPanel, RestoreConfirmModal } from './components'
