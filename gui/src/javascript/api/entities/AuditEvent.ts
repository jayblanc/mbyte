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

type AuditEventData = {
  id: string;
  timeStamp: string;
  userId: string;
  storeId: string;
  action: string;
  status: string;
  method: string;
  path: string;
  service: string;
};

/* Safe default: if backend data is broken or missing, getters still return empty strings (not undefined) */
const EMPTY_AUDIT_EVENT: AuditEventData = {
  id: "",
  timeStamp: "",
  userId: "",
  storeId: "",
  action: "",
  status: "",
  method: "",
  path: "",
  service: "",
};

/* Check if the value from the backend is a real object (not null, not a number, etc.) */
function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === "object";
}

/* Safely convert any value to a string. If not a string, return empty string instead */
function readString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

/* Transform raw backend data into a clean, safe structure with all fields as strings.
 * Do all safety checks here, once, so the rest of the code doesn't need to worry about bad data.
 */
function normalizeAuditEvent(dto: unknown): AuditEventData {
  if (!isRecord(dto)) return EMPTY_AUDIT_EVENT;

  return {
    id: readString(dto.id),
    timeStamp: readString(dto.timeStamp),
    userId: readString(dto.userId),
    storeId: readString(dto.storeId),
    action: readString(dto.action),
    status: readString(dto.status),
    method: readString(dto.method),
    path: readString(dto.path),
    service: readString(dto.service),
  };
}

export default class AuditEvent { 
  readonly dto: AuditEventData;

  constructor(dto: AuditEventData) {
    this.dto = dto;
  }

  static fromDto(dto: unknown): AuditEvent {
    return new AuditEvent(normalizeAuditEvent(dto));
  }

  get id(): string {
    return this.dto.id;
  }

  get timeStamp(): string {
    return this.dto.timeStamp;
  }

  get timeStampMs(): number | undefined {
    /* Change timestamp from text format to milliseconds so we can sort and compare dates in the UI */
    if (!this.dto.timeStamp) return undefined;
    const parsed = Date.parse(this.dto.timeStamp);
    return Number.isNaN(parsed) ? undefined : parsed;
  }

  get userId(): string {
    return this.dto.userId;
  }

  get storeId(): string {
    return this.dto.storeId;
  }

  get action(): string {
    return this.dto.action;
  }

  get status(): string {
    return this.dto.status;
  }

  get method(): string {
    return this.dto.method;
  }

  get path(): string {
    return this.dto.path;
  }

  get service(): string {
    return this.dto.service;
  }
}