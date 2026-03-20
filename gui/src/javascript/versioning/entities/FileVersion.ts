export type FileVersionData = {
  id: string
  nodeId: string
  nodeName: string
  author: string
  timestamp: number
  versionNumber?: number
  mimetype?: string
  size?: number
  comment?: string
}

export default class FileVersion {
  readonly data: FileVersionData

  constructor(data: FileVersionData) {
    this.data = data
  }

  static fromData(data: FileVersionData): FileVersion {
    return new FileVersion(data)
  }

  static fromDto(dto: any): FileVersion {
    return new FileVersion({
      id: dto.id,
      nodeId: dto.nodeId,
      nodeName: dto.nodeName,
      author: dto.author ?? 'unknown',
      timestamp: dto.createdAt ? new Date(dto.createdAt).getTime() : Date.now(),
      versionNumber: dto.versionNumber,
      mimetype: dto.mimetype,
      size: dto.size,
      comment: dto.comment,
    })
  }

  get id(): string {
    return this.data.id
  }

  get nodeId(): string {
    return this.data.nodeId
  }

  get nodeName(): string {
    return this.data.nodeName
  }

  get author(): string {
    return this.data.author
  }

  get timestamp(): number {
    return this.data.timestamp
  }

  get versionNumber(): number | undefined {
    return this.data.versionNumber
  }

  get comment(): string | undefined {
    return this.data.comment
  }

  get formattedDate(): string {
    return new Date(this.timestamp).toLocaleString()
  }
}
