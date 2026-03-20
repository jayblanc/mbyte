
import FileVersion from './entities/FileVersion'

export type FetchFn = (path: string, init?: RequestInit) => Promise<Response>

export function createVersioningService(fetchFn: FetchFn) {
  return {
    async createVersion(
      nodeId: string,
      nodeName: string,
      content: Blob,
      _author?: string,
      comment?: string
    ): Promise<FileVersion> {
      const fd = new FormData()
      fd.append('name', nodeName)
      fd.append('file', content)
      fd.append('mimetype', content.type || 'application/octet-stream')
      fd.append('size', String(content.size))
      if (comment) fd.append('comment', comment)

      const res = await fetchFn(`/api/versions/node/${encodeURIComponent(nodeId)}`, {
        method: 'POST',
        body: fd,
      })
      if (!res.ok) {
        const text = await res.text()
        throw new Error(`Create version failed (${res.status}): ${text}`)
      }
      const dto = await res.json()
      return FileVersion.fromDto(dto)
    },

    async getVersionHistory(nodeId: string): Promise<FileVersion[]> {
      const res = await fetchFn(`/api/versions/node/${encodeURIComponent(nodeId)}`, {
        method: 'GET',
      })
      if (!res.ok) {
        const text = await res.text()
        throw new Error(`Get history failed (${res.status}): ${text}`)
      }
      const arr = await res.json()
      return (arr as any[]).map((dto) => FileVersion.fromDto(dto))
    },

    async getVersion(versionId: string): Promise<FileVersion | null> {
      const res = await fetchFn(`/api/versions/${encodeURIComponent(versionId)}`, {
        method: 'GET',
      })
      if (res.status === 404) return null
      if (!res.ok) {
        const text = await res.text()
        throw new Error(`Get version failed (${res.status}): ${text}`)
      }
      const dto = await res.json()
      return FileVersion.fromDto(dto)
    },

    async deleteVersion(versionId: string): Promise<void> {
      const res = await fetchFn(`/api/versions/${encodeURIComponent(versionId)}`, {
        method: 'DELETE',
      })
      if (!res.ok) {
        const text = await res.text()
        throw new Error(`Delete version failed (${res.status}): ${text}`)
      }
    },

    async getVersionCount(nodeId: string): Promise<number> {
      const res = await fetchFn(`/api/versions/node/${encodeURIComponent(nodeId)}`, {
        method: 'GET',
      })
      if (!res.ok) return 0
      const arr = await res.json()
      return (arr as any[]).length
    },

    async getVersionContent(versionId: string): Promise<Response> {
      return fetchFn(`/api/versions/${encodeURIComponent(versionId)}/content`, {
        method: 'GET',
      })
    },
  }
}

export type VersioningService = ReturnType<typeof createVersioningService>
