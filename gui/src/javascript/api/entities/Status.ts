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

export default class Status {
  connectedId: string
  nbCpus: number
  totalMemory: number
  availableMemory: number
  maxMemory: number
  latestMetrics: Map<string, number>
  metrics: Map<string, number>

  constructor(
    connectedId: string,
    nbCpus: number,
    totalMemory: number,
    availableMemory: number,
    maxMemory: number,
    latestMetrics: Map<string, number>,
    metrics: Map<string, number>
  ) {
    this.connectedId = connectedId
    this.nbCpus = nbCpus
    this.totalMemory = totalMemory
    this.availableMemory = availableMemory
    this.maxMemory = maxMemory
    this.latestMetrics = latestMetrics
    this.metrics = metrics
  }

  static fromDto(dto: any): Status {
    return new Status(
      dto.connectedId,
      dto.nbCpus,
      dto.totalMemory,
      dto.availableMemory,
      dto.maxMemory,
      new Map(Object.entries(dto.latestMetrics || {})),
      new Map(Object.entries(dto.metrics || {}))
    )
  }
}
