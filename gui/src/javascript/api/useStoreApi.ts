///
/// Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
///
/// This program is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as published by
/// the Free Software Foundation, either version 3 of the License, or
/// (at your option) any later version.
///
/// This program is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with this program.  If not, see <https://www.gnu.org/licenses/>.
///

import { useMemo } from 'react'
import { createStoreApi } from './storeApi'
import type { TokenProvider } from './fetchWithAuth'

// Now accepts an optional baseUrlOverride so the UI can pass a per-user store base URL.
export function useStoreApi(tokenProvider: TokenProvider, baseUrlOverride?: string) {
  const api = useMemo(() => createStoreApi(tokenProvider, baseUrlOverride ? { baseUrlOverride } : {}), [tokenProvider, baseUrlOverride])

  const wrap = <T extends (...args: any[]) => Promise<any>>(fn: T) => {
    return (async (...args: Parameters<T>): Promise<ReturnType<T>> => {
      try {
        // @ts-ignore
        return await fn(...args)
      } catch (err: any) {
        const msg = err?.message ?? String(err)
        // Don't show toast for "already-exists" errors — handled by the UI
        if (!msg.includes('already-exists')) {
          globalThis.dispatchEvent(new CustomEvent('mbyte-toast', { detail: { message: msg } }))
        }
        throw err
      }
    }) as T
  }

  return useMemo(() => {
    const out: any = {}
    Object.keys(api).forEach((k) => {
      const v: any = (api as any)[k]
      if (typeof v === 'function') {
        out[k] = wrap(v.bind(api))
      } else {
        out[k] = v
      }
    })
    return out as typeof api
  }, [api])
}
