
export type FileSavedEventDetail = {
  nodeId: string
  nodeName: string
  content: Blob
  author: string
}

export type VersionCreatedEventDetail = {
  versionId: string
  nodeId: string
  nodeName: string
  versionNumber: number
}

export type VersionRestoredEventDetail = {
  versionId: string
  nodeId: string
  nodeName: string
}

export const VERSIONING_EVENTS = {
  FILE_SAVED: 'mbyte-file-saved',
  VERSION_CREATED: 'mbyte-version-created',
  VERSION_RESTORED: 'mbyte-version-restored',
} as const

export function emitFileSaved(detail: FileSavedEventDetail) {
  globalThis.dispatchEvent(new CustomEvent(VERSIONING_EVENTS.FILE_SAVED, { detail }))
}

export function emitVersionCreated(detail: VersionCreatedEventDetail) {
  globalThis.dispatchEvent(new CustomEvent(VERSIONING_EVENTS.VERSION_CREATED, { detail }))
}

export function emitVersionRestored(detail: VersionRestoredEventDetail) {
  globalThis.dispatchEvent(new CustomEvent(VERSIONING_EVENTS.VERSION_RESTORED, { detail }))
}

export function onFileSaved(handler: (detail: FileSavedEventDetail) => void): () => void {
  const listener = (event: Event) => {
    const ce = event as CustomEvent<FileSavedEventDetail>
    handler(ce.detail)
  }
  globalThis.addEventListener(VERSIONING_EVENTS.FILE_SAVED, listener)
  return () => globalThis.removeEventListener(VERSIONING_EVENTS.FILE_SAVED, listener)
}

export function onVersionCreated(handler: (detail: VersionCreatedEventDetail) => void): () => void {
  const listener = (event: Event) => {
    const ce = event as CustomEvent<VersionCreatedEventDetail>
    handler(ce.detail)
  }
  globalThis.addEventListener(VERSIONING_EVENTS.VERSION_CREATED, listener)
  return () => globalThis.removeEventListener(VERSIONING_EVENTS.VERSION_CREATED, listener)
}

export function onVersionRestored(handler: (detail: VersionRestoredEventDetail) => void): () => void {
  const listener = (event: Event) => {
    const ce = event as CustomEvent<VersionRestoredEventDetail>
    handler(ce.detail)
  }
  globalThis.addEventListener(VERSIONING_EVENTS.VERSION_RESTORED, listener)
  return () => globalThis.removeEventListener(VERSIONING_EVENTS.VERSION_RESTORED, listener)
}
