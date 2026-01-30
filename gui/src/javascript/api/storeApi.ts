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

import { fetchWithAuth, readJsonOrThrow, type TokenProvider } from './fetchWithAuth'
import { apiConfig } from './apiConfig'
import Node from './entities/Node'
import Neighbour from './entities/Neighbour'
import Status from './entities/Status'
import SearchResult from './entities/SearchResult'

export type StoreLocator = {
  /** User login used to compute <login>.<storesDomain> */
  username: string
  /** Forward-compatible hook: multiple stores could exist per profile */
  index?: number
}

export function computeStoreBaseUrl(locator: StoreLocator): string {
  const username = locator.username.trim()
  if (!username) {
    throw new Error('Cannot compute store base URL: empty username')
  }

  const host = `${username}.${apiConfig.storesDomain}`
  const scheme = apiConfig.storesScheme

  // Ensure trailing slash so new URL('/q/health', baseUrl) works as expected
  return `${scheme}://${host}/`
}

export type CreateStoreApiOptions = {
  /** Optional explicit base URL (overrides env + computed FQDN) */
  baseUrlOverride?: string
  /** Used to compute the store FQDN when baseUrlOverride + env baseUrl are not provided */
  storeLocator?: StoreLocator
}

export function createStoreApi(tokenProvider: TokenProvider, options: CreateStoreApiOptions = {}) {
  const baseUrl =
    options.baseUrlOverride ??
    apiConfig.storeBaseUrl ??
    (options.storeLocator ? computeStoreBaseUrl(options.storeLocator) : undefined)

  return {
    // expose whether the API has a base URL configured (useful for UI to avoid repeated failing calls)
    isConfigured: Boolean(baseUrl),

    async getHealth(): Promise<unknown> {
      if (!baseUrl) {
        throw new Error(
          'Store base URL is not configured. Set VITE_API_STORE_BASE_URL or provide storeLocator (username + STORES_DOMAIN).',
        )
      }
      const res = await fetchWithAuth(tokenProvider, '/q/health', { method: 'GET' }, baseUrl)
      const text = await res.text()
      if (!res.ok) {
        throw new Error(`Store health failed (${res.status}): ${text}`)
      }
      try {
        return JSON.parse(text)
      } catch {
        return text
      }
    },

    /** Get the root node by calling GET /api/nodes (server replies with a redirect to the real root id). */
    async getRoot(): Promise<Node> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      // debug
      // eslint-disable-next-line no-console
      console.debug('[storeApi] getRoot ->', baseUrl)
      const res = await fetchWithAuth(tokenProvider, '/api/nodes', { method: 'GET' }, baseUrl)
      const dto = (await readJsonOrThrow(res)) as any
      return Node.fromDto(dto)
    },

    // --- Nodes API (JSON endpoints)
    /** Represents a node returned by the store API */
    // Note: creation/modification are ISO date strings from the backend
    // Keep them as strings here so callers can parse as needed.
    async getNode(id: string): Promise<Node> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      // debug
      // eslint-disable-next-line no-console
      console.debug('[storeApi] getNode', id, '->', baseUrl)
      const res = await fetchWithAuth(tokenProvider, `/api/nodes/${encodeURIComponent(id)}`, { method: 'GET' }, baseUrl)
      const dto = (await readJsonOrThrow(res)) as any
      return Node.fromDto(dto)
    },

    async listChildren(id: string, limit = 20, offset = 0): Promise<CollectionDto<Node>> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      // debug
      // eslint-disable-next-line no-console
      console.debug('[storeApi] listChildren', id, 'limit=', limit, 'offset=', offset, '->', baseUrl)
      const query = `?limit=${encodeURIComponent(String(limit))}&offset=${encodeURIComponent(String(offset))}`
      const res = await fetchWithAuth(
        tokenProvider,
        `/api/nodes/${encodeURIComponent(id)}/children${query}`,
        { method: 'GET' },
        baseUrl,
      )
      const coll = (await readJsonOrThrow(res)) as any
      return {
        limit: coll.limit,
        offset: coll.offset,
        size: coll.size,
        values: (coll.values || []).map((d: any) => Node.fromDto(d)),
      }
    },

    /** Get the path (list of nodes from root to the given node id) */
    async getPath(id: string): Promise<Node[]> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      // debug
      // eslint-disable-next-line no-console
      console.debug('[storeApi] getPath', id, '->', baseUrl)
      const res = await fetchWithAuth(tokenProvider, `/api/nodes/${encodeURIComponent(id)}/path`, { method: 'GET' }, baseUrl)
      const arr = (await readJsonOrThrow(res)) as any[]
      return (arr || []).map((d: any) => Node.fromDto(d))
    },

    /** Get node content. Returns the raw Response so caller can decide to read a blob or stream.
     * If download=true the server will set Content-Disposition to attachment.
     */
    async content(id: string, download = false): Promise<Response> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      // debug
      // eslint-disable-next-line no-console
      console.debug('[storeApi] content', id, 'download=', download, '->', baseUrl)
      const q = download ? '?download=true' : ''
      const res = await fetchWithAuth(tokenProvider, `/api/nodes/${encodeURIComponent(id)}/content${q}`, { method: 'GET' }, baseUrl)
      return res
    },

    /**
     * Create a node (file or folder) under parent `id`.
     * If `file` is provided it will POST multipart/form-data with fields 'data' and 'name'.
     * If no file is provided it will POST JSON { name } to create an empty folder.
     * Returns the created resource URI (Location header) or null.
     */
    async create(parentId: string, name: string, file?: File | Blob): Promise<string | null> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      if (file) {
        const fd = new FormData()
        fd.append('name', name)
        fd.append('data', file)
        const res = await fetchWithAuth(tokenProvider, `/api/nodes/${encodeURIComponent(parentId)}`, { method: 'POST', body: fd }, baseUrl)
        if (!res.ok) {
          const text = await res.text()
          throw new Error(`Create failed (${res.status}): ${text}`)
        }
        return res.headers.get('Location')
      } else {
        const body = JSON.stringify({ name })
        const res = await fetchWithAuth(tokenProvider, `/api/nodes/${encodeURIComponent(parentId)}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body }, baseUrl)
        if (!res.ok) {
          const text = await res.text()
          throw new Error(`Create failed (${res.status}): ${text}`)
        }
        return res.headers.get('Location')
      }
    },

    /**
     * Update (create a new version) of an existing node identified by parent id and name.
     * Expects multipart/form-data with field 'data'.
     */
    async update(parentId: string, name: string, file: File | Blob): Promise<void> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      const fd = new FormData()
      fd.append('data', file)
      const path = `/api/nodes/${encodeURIComponent(parentId)}/${encodeURIComponent(name)}`
      const res = await fetchWithAuth(tokenProvider, path, { method: 'PUT', body: fd }, baseUrl)
      if (!res.ok) {
        const text = await res.text()
        throw new Error(`Update failed (${res.status}): ${text}`)
      }
    },

    /**
     * Delete a child (file or folder) named `name` under parent `id`.
     */
    async delete(parentId: string, name: string): Promise<void> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      const path = `/api/nodes/${encodeURIComponent(parentId)}/${encodeURIComponent(name)}`
      const res = await fetchWithAuth(tokenProvider, path, { method: 'DELETE' }, baseUrl)
      if (!res.ok) {
        const text = await res.text()
        throw new Error(`Delete failed (${res.status}): ${text}`)
      }
    },

    async getNeighbours(): Promise<Neighbour[]> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      const res = await fetchWithAuth(tokenProvider, '/api/network', { method: 'GET' }, baseUrl)
      const arr = (await readJsonOrThrow(res)) as any[]
      return (arr || []).map((d: any) => Neighbour.fromDto(d))
    },

    async getStatus(): Promise<Status> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      const res = await fetchWithAuth(tokenProvider, '/api/status', { method: 'GET' }, baseUrl)
      const dto = (await readJsonOrThrow(res)) as any
      return Status.fromDto(dto)
    },

    async search(query: string): Promise<SearchResult[]> {
      if (!baseUrl) throw new Error('Store base URL is not configured')
      const q = `?q=${encodeURIComponent(query)}`
      const res = await fetchWithAuth(tokenProvider, `/api/search${q}`, { method: 'GET' }, baseUrl)
      const arr = (await readJsonOrThrow(res)) as any[]
      return (arr || []).map((d: any) => SearchResult.fromDto(d))
    },
  }
}

// --- Types exposed by the store API
export type CollectionDto<T> = {
  limit: number
  offset: number
  size: number
  values: T[]
}
