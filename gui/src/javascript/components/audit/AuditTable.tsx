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

import {
  CBadge,
  CSpinner,
  CTable,
  CTableBody,
  CTableDataCell,
  CTableHead,
  CTableHeaderCell,
  CTableRow,
} from "@coreui/react";
import { useTranslation } from "react-i18next";
import AuditEvent from "../../api/entities/AuditEvent";

type AuditTableProps = {
  loading: boolean;
  events: AuditEvent[];
};

function asLocalDateString(tsMs: number | undefined): string {
  if (!tsMs) return "-";
  return new Intl.DateTimeFormat(undefined, {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(tsMs);
}

function statusColor(status: string): "success" | "danger" | "warning" {
  const upper = status.toUpperCase();
  if (upper === "SUCCESS") return "success";
  if (upper === "FAILURE") return "danger";
  return "warning";
}

function methodColor(
  method: string,
): "primary" | "success" | "warning" | "danger" | "secondary" {
  const upper = method.toUpperCase();
  if (upper === "GET") return "primary";
  if (upper === "POST") return "success";
  if (upper === "PUT") return "warning";
  if (upper === "DELETE") return "danger";
  return "secondary";
}

export function AuditTable({ loading, events }: AuditTableProps) {
  const { t } = useTranslation();

  if (loading) {
    return (
      <div className="d-flex align-items-center justify-content-center py-4">
        <CSpinner />
      </div>
    );
  }

  return (
    <div className="table-responsive">
      <CTable hover align="middle" className="mb-0 audit-table" small>
        <CTableHead>
          <CTableRow>
            <CTableHeaderCell>{t("audit.columns.timestamp")}</CTableHeaderCell>
            <CTableHeaderCell>{t("audit.columns.user")}</CTableHeaderCell>
            <CTableHeaderCell>{t("audit.columns.action")}</CTableHeaderCell>
            <CTableHeaderCell>{t("audit.columns.status")}</CTableHeaderCell>
            <CTableHeaderCell>{t("audit.columns.method")}</CTableHeaderCell>
            <CTableHeaderCell>{t("audit.columns.path")}</CTableHeaderCell>
          </CTableRow>
        </CTableHead>
        <CTableBody>
          {events.length === 0 && (
            <CTableRow>
              <CTableDataCell
                colSpan={6}
                className="text-center text-body-secondary py-4"
              >
                {t("audit.empty")}
              </CTableDataCell>
            </CTableRow>
          )}

          {events.map((event) => (
            <CTableRow key={event.id || `${event.timeStamp}-${event.path}`}>
              <CTableDataCell>
                {asLocalDateString(event.timeStampMs)}
              </CTableDataCell>
              <CTableDataCell>{event.userId || "-"}</CTableDataCell>
              <CTableDataCell>
                <CBadge color="info" className="audit-badge-action">
                  {event.action || "-"}
                </CBadge>
              </CTableDataCell>
              <CTableDataCell>
                <CBadge color={statusColor(event.status)}>
                  {event.status || "-"}
                </CBadge>
              </CTableDataCell>
              <CTableDataCell>
                <CBadge
                  color={methodColor(event.method)}
                  className="audit-badge-method"
                >
                  {event.method || "-"}
                </CBadge>
              </CTableDataCell>
              <CTableDataCell className="audit-path">
                {event.path || "-"}
              </CTableDataCell>
            </CTableRow>
          ))}
        </CTableBody>
      </CTable>
    </div>
  );
}
