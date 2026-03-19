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

import {fetchWithAuth, type TokenProvider} from './fetchWithAuth'
import {apiConfig} from './apiConfig'
import type {Profile} from './entities/Profile'
import type {Process} from './entities/Process'
import type {ManagerStatus} from './entities/ManagerStatus'
import type {Application} from './entities/Application'
import type { CommandDescriptor } from './entities/CommandDescriptor'

async function readJsonOrThrow(res: Response): Promise<unknown> {
  const text = await res.text()
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${text}`)
  }
  try {
    return text ? JSON.parse(text) : null
  } catch {
    return text
  }
}

export function createManagerApi(tokenProvider: TokenProvider) {
  const baseUrl = apiConfig.managerBaseUrl

  const requireBaseUrl = () => {
    if (!baseUrl) {
      throw new Error('VITE_API_MANAGER_BASE_URL is not set')
    }
    return baseUrl
  }

  return {
    async getHealth(): Promise<unknown> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, '/q/health', { method: 'GET' }, base)
      return readJsonOrThrow(res)
    },

    /**
     * Returns the current user's Profile.
     * The backend endpoint (/api/profiles/me) may reply with a redirect to /api/profiles/{id}.
     * Fetch follows redirects by default, so a single call is enough.
     */
    async getCurrentProfile(): Promise<Profile> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, '/api/profiles/me', { method: 'GET' }, base)
      return (await readJsonOrThrow(res)) as Profile
    },

    /** Lists applications (GET /api/apps?owner=...). */
    async listApps(owner?: string): Promise<Application[]> {
      const base = requireBaseUrl()
      const url = owner ? `/api/apps?owner=${encodeURIComponent(owner)}` : '/api/apps'
      const res = await fetchWithAuth(tokenProvider, url, { method: 'GET' }, base)
      return (await readJsonOrThrow(res)) as Application[]
    },

    /** Returns an application by id (GET /api/apps/{id}). */
    async getApp(appId: string): Promise<Application> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, `/api/apps/${encodeURIComponent(appId)}`, { method: 'GET' }, base)
      return (await readJsonOrThrow(res)) as Application
    },

    /** Deletes an application (DELETE /api/apps/{id}). Idempotent on backend. */
    async deleteApp(appId: string): Promise<void> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, `/api/apps/${encodeURIComponent(appId)}`, { method: 'DELETE' }, base)
      if (!res.ok && res.status !== 404) {
        const text = await res.text()
        throw new Error(`Delete failed (${res.status}): ${text}`)
      }
    },

    /** Creates an application (POST /api/apps). Returns created id. */
    async createApp(type: string, name: string): Promise<string> {
      const base = requireBaseUrl()
      const body = new URLSearchParams({ type, name })
      const res = await fetchWithAuth(
        tokenProvider,
        '/api/apps',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body,
        },
        base,
      )
      const json = (await readJsonOrThrow(res)) as { id?: string }
      if (!json?.id) {
        throw new Error('Invalid createApp response: missing id')
      }
      return json.id
    },

    /** Returns processes for an app (GET /api/apps/{appId}/procs?active=true|false). */
    async getAppProcs(appId: string, active = true): Promise<Process[]> {
        const base = requireBaseUrl()
        const url = active ? `/api/apps/${encodeURIComponent(appId)}/procs?active=true` : `/api/apps/${encodeURIComponent(appId)}/procs`
        const res = await fetchWithAuth(tokenProvider, url, { method: 'GET' }, base)
        return (await readJsonOrThrow(res)) as Process[]
    },

    /** Returns available command names for an app (GET /api/apps/{appId}/commands). */
    async listAppCommands(appId: string): Promise<CommandDescriptor[]> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, `/api/apps/${encodeURIComponent(appId)}/commands`, { method: 'GET' }, base)
      return (await readJsonOrThrow(res)) as CommandDescriptor[]
    },

    /** Runs a command on an app (POST /api/apps/{appId}/procs). Returns process id. */
    async runAppCommand(appId: string, commandName: string): Promise<string> {
      const base = requireBaseUrl()
      const body = new URLSearchParams({ name: commandName })
      const res = await fetchWithAuth(
        tokenProvider,
        `/api/apps/${encodeURIComponent(appId)}/procs`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body,
        },
        base,
      )
      const result = await readJsonOrThrow(res)
      if (typeof result === 'string') {
        return result
      } else if (typeof result === 'object' && result && 'id' in result) {
        return (result as { id: string }).id
      } else {
        throw new Error('Invalid runAppCommand response: missing id')
      }
    },

    /** Returns a specific process for an app (GET /api/apps/{appId}/procs/{procId}). */
    async getAppProc(appId: string, procId: string): Promise<Process> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, `/api/apps/${encodeURIComponent(appId)}/procs/${encodeURIComponent(procId)}`, { method: 'GET' }, base)
      return (await readJsonOrThrow(res)) as Process
    },

    async getStatus(): Promise<ManagerStatus> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, '/api/status', { method: 'GET' }, base)
      return (await readJsonOrThrow(res)) as ManagerStatus
    },
  }
}
