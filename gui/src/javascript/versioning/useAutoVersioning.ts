
import { useEffect } from 'react'
import { useVersioningService } from './useVersioningService'
import {
  onFileSaved,
  emitVersionCreated,
  type FileSavedEventDetail,
} from './versioningEvents'
import type { FetchFn } from './versioningService'

export function useAutoVersioning(fetchFn: FetchFn) {
  const versioningService = useVersioningService(fetchFn)

  useEffect(() => {
    const handleFileSaved = async (detail: FileSavedEventDetail) => {
      try {
        const version = await versioningService.createVersion(
          detail.nodeId,
          detail.nodeName,
          detail.content,
          detail.author
        )

        const count = await versioningService.getVersionCount(detail.nodeId)

        emitVersionCreated({
          versionId: version.id,
          nodeId: detail.nodeId,
          nodeName: detail.nodeName,
          versionNumber: count,
        })

        globalThis.dispatchEvent(
          new CustomEvent('mbyte-toast', {
            detail: { message: `Version #${count} enregistrée` },
          })
        )
      } catch (err) {
        console.error('[versioning] Failed to create version', err)
      }
    }

    const unsubscribe = onFileSaved(handleFileSaved)
    return () => { unsubscribe() }
  }, [versioningService])
}
