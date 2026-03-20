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

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  CCard,
  CCardBody,
  CCardHeader,
  CCol,
  CContainer,
  CRow,
} from "@coreui/react";
import { useTranslation } from "react-i18next";
import { useAccessToken } from "../auth/useAccessToken";
import { useStoreApi } from "../api/useStoreApi";
import { useManagerStatus } from "../auth/useManagerStatus";
import { apiConfig } from "../api/apiConfig";
import AuditEvent from "../api/entities/AuditEvent";
import { AuditTable } from "../components/audit/AuditTable";
import { AuditToolbar } from "../components/audit/AuditToolbar";
import { AuditPagination } from "../components/audit/AuditPagination";

export function AuditPage() {
  const { t } = useTranslation();
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [serverLimit, setServerLimit] = useState(200);
  const pageSize = 25;
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [showFilters, setShowFilters] = useState(false);
  const [statusFilter, setStatusFilter] = useState("");
  const [methodFilter, setMethodFilter] = useState("");
  const [actionFilter, setActionFilter] = useState("");

  const tokenProvider = useAccessToken();
  const { apps } = useManagerStatus();
  const userStoreApp = apps.find((a) => a?.type === "DOCKER_STORE");

  const storeBaseUrl = useMemo(() => {
    if (!userStoreApp?.name) return undefined;
    return `${apiConfig.storesScheme}://${userStoreApp.name}.${apiConfig.storesDomain}/`;
  }, [userStoreApp?.name]);

  const storeApi = useStoreApi(tokenProvider, storeBaseUrl);

  const loadAudits = useCallback(async () => {
    if (!storeApi.isConfigured) {
      setEvents([]);
      return;
    }
    setLoading(true);
    try {
      const list = await storeApi.listAudits(serverLimit);
      setEvents(list);
    } finally {
      setLoading(false);
    }
  }, [serverLimit, storeApi]);

  useEffect(() => {
    void loadAudits();
  }, [loadAudits]);

  /* Filter events based on search query. This is done client-side for simplicity, but could be optimized by sending the query to the backend if needed. */
  const filteredEvents = useMemo(() => {
    const query = search.trim().toLowerCase();
    return events.filter((event) => {
      const matchesSearch =
        !query ||
        [event.userId, event.action, event.status, event.method, event.path]
          .join(" ")
          .toLowerCase()
          .includes(query);

      const matchesStatus =
        !statusFilter || event.status.toUpperCase() === statusFilter;
      const matchesMethod =
        !methodFilter || event.method.toUpperCase() === methodFilter;
      const matchesAction =
        !actionFilter || event.action.toUpperCase() === actionFilter;

      return matchesSearch && matchesStatus && matchesMethod && matchesAction;
    });
  }, [events, search, statusFilter, methodFilter, actionFilter]);

  /* Calculate total pages based on filtered events and page size. If there are no events, we still want to show 1 page (with "no results" message) rather than 0 pages. */
  const totalPages = useMemo(() => {
    if (filteredEvents.length === 0) return 1;
    return Math.ceil(filteredEvents.length / pageSize);
  }, [filteredEvents.length, pageSize]);

  useEffect(() => {
    setPage(1);
  }, [search, serverLimit, statusFilter, methodFilter, actionFilter]);

  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [page, totalPages]);

  const pagedEvents = useMemo(() => {
    const start = (page - 1) * pageSize;
    return filteredEvents.slice(start, start + pageSize);
  }, [filteredEvents, page, pageSize]);

  const statusOptions = useMemo(
    () =>
      Array.from(
        new Set(
          events.map((event) => event.status.toUpperCase()).filter(Boolean),
        ),
      ).sort(),
    [events],
  );
  const methodOptions = useMemo(
    () =>
      Array.from(
        new Set(
          events.map((event) => event.method.toUpperCase()).filter(Boolean),
        ),
      ).sort(),
    [events],
  );
  const actionOptions = useMemo(
    () =>
      Array.from(
        new Set(
          events.map((event) => event.action.toUpperCase()).filter(Boolean),
        ),
      ).sort(),
    [events],
  );

  const activeFilterCount = useMemo(
    () => [statusFilter, methodFilter, actionFilter].filter(Boolean).length,
    [statusFilter, methodFilter, actionFilter],
  );

  if (!storeApi.isConfigured) {
    return (
      <CContainer fluid className="py-3">
        <CCard>
          <CCardHeader>{t("audit.title")}</CCardHeader>
          <CCardBody>{t("audit.notConfigured")}</CCardBody>
        </CCard>
      </CContainer>
    );
  }

  return (
    <CContainer fluid className="py-3">
      <CRow className="g-3">
        <CCol xs={12}>
          <CCard>
            <CCardHeader className="d-flex align-items-center justify-content-between">
              <strong>{t("audit.title")}</strong>
              <small className="text-body-secondary">
                {t("audit.totalFetched", { count: events.length })} •{" "}
                {t("audit.totalDisplayed", { count: filteredEvents.length })}
              </small>
            </CCardHeader>
            <CCardBody className="audit-page__body">
              <AuditToolbar
                serverLimit={serverLimit}
                search={search}
                loading={loading}
                showFilters={showFilters}
                activeFilterCount={activeFilterCount}
                statusFilter={statusFilter}
                methodFilter={methodFilter}
                actionFilter={actionFilter}
                statusOptions={statusOptions}
                methodOptions={methodOptions}
                actionOptions={actionOptions}
                onServerLimitChange={setServerLimit}
                onSearchChange={setSearch}
                onToggleFilters={() => setShowFilters((value) => !value)}
                onStatusFilterChange={setStatusFilter}
                onMethodFilterChange={setMethodFilter}
                onActionFilterChange={setActionFilter}
                onClearFilters={() => {
                  setStatusFilter("");
                  setMethodFilter("");
                  setActionFilter("");
                }}
                onRefresh={() => void loadAudits()}
              />

              <AuditTable loading={loading} events={pagedEvents} />

              <AuditPagination
                page={page}
                totalPages={totalPages}
                totalItems={filteredEvents.length}
                pageSize={pageSize}
                onPageChange={setPage}
              />
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>
    </CContainer>
  );
}
