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

export default class Neighbour {
  id: string
  name: string
  address: string
  fqdn: string

  constructor(id: string, name: string, address: string, fqdn: string) {
    this.id = id
    this.name = name
    this.address = address
    this.fqdn = fqdn
  }

  static fromDto(dto: any): Neighbour {
    return new Neighbour(dto.id, dto.name, dto.address, dto.fqdn)
  }
}
