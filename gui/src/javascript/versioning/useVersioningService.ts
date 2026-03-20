
import { useMemo } from 'react'
import { createVersioningService, type VersioningService, type FetchFn } from './versioningService'

function dispatchToast(message: string) {
  globalThis.dispatchEvent(new CustomEvent('mbyte-toast', { detail: { message } }))
}

export function useVersioningService(fetchFn: FetchFn) {
  const service = useMemo(() => createVersioningService(fetchFn), [fetchFn])

  const wrap = <T extends (...args: any[]) => Promise<any>>(fn: T) => {
    return (async (...args: Parameters<T>): Promise<ReturnType<T>> => {
      try {
        return await fn(...args)
      } catch (err: any) {
        const msg = err?.message ?? String(err)
        dispatchToast(msg)
        throw err
      }
    }) as T
  }

  return useMemo(() => {
    const out: any = {}
    Object.keys(service).forEach((k) => {
      const v: any = (service as any)[k]
      if (typeof v === 'function') {
        out[k] = wrap(v.bind(service))
      } else {
        out[k] = v
      }
    })
    return out as VersioningService
  }, [service])
}
