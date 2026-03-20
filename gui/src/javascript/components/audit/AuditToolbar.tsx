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
  CButton,
  CFormInput,
  CFormLabel,
  CFormSelect,
} from "@coreui/react";
import CIcon from "@coreui/icons-react";
import { cilFilter } from "@coreui/icons";
import { useTranslation } from "react-i18next";

type AuditToolbarProps = {
  serverLimit: number;
  search: string;
  loading: boolean;
  showFilters: boolean;
  activeFilterCount: number;
  statusFilter: string;
  methodFilter: string;
  actionFilter: string;
  statusOptions: string[];
  methodOptions: string[];
  actionOptions: string[];
  onServerLimitChange: (value: number) => void;
  onSearchChange: (value: string) => void;
  onToggleFilters: () => void;
  onStatusFilterChange: (value: string) => void;
  onMethodFilterChange: (value: string) => void;
  onActionFilterChange: (value: string) => void;
  onClearFilters: () => void;
  onRefresh: () => void;
};

export function AuditToolbar({
  serverLimit,
  search,
  loading,
  showFilters,
  activeFilterCount,
  statusFilter,
  methodFilter,
  actionFilter,
  statusOptions,
  methodOptions,
  actionOptions,
  onServerLimitChange,
  onSearchChange,
  onToggleFilters,
  onStatusFilterChange,
  onMethodFilterChange,
  onActionFilterChange,
  onClearFilters,
  onRefresh,
}: AuditToolbarProps) {
  const { t } = useTranslation();

  return (
    <div className="audit-toolbar d-flex flex-wrap align-items-center gap-2">
      <CFormInput
        size="sm"
        value={search}
        onChange={(event) => onSearchChange(event.target.value)}
        placeholder={t("audit.searchPlaceholder")}
        className="audit-toolbar__search"
        aria-label={t("audit.searchPlaceholder")}
      />

      <div className="d-flex align-items-center gap-2">
        <CFormLabel htmlFor="audit-limit" className="mb-0 text-body-secondary">
          {t("audit.limit")}
        </CFormLabel>
        <CFormSelect
          id="audit-limit"
          size="sm"
          value={String(serverLimit)}
          onChange={(event) => onServerLimitChange(Number(event.target.value))}
          aria-label={t("audit.limit")}
        >
          <option value="50">50</option>
          <option value="100">100</option>
          <option value="200">200</option>
          <option value="500">500</option>
        </CFormSelect>
      </div>

      <CButton
        color={showFilters ? "primary" : "light"}
        size="sm"
        onClick={onToggleFilters}
        className="audit-toolbar__filters-btn"
      >
        <CIcon icon={cilFilter} className="me-1" />
        {t("audit.filters.button")}
        {activeFilterCount > 0 && (
          <CBadge color={showFilters ? "light" : "primary"} className="ms-2">
            {activeFilterCount}
          </CBadge>
        )}
      </CButton>

      <CButton color="light" size="sm" onClick={onRefresh} disabled={loading}>
        {loading ? t("audit.loading") : t("audit.refresh")}
      </CButton>

      {showFilters && (
        <div className="audit-toolbar__filters-panel w-100 mt-2">
          <div className="d-flex flex-wrap align-items-end gap-2">
            <div className="audit-filter-item">
              <CFormLabel
                htmlFor="audit-filter-status"
                className="mb-1 text-body-secondary"
              >
                {t("audit.filters.status")}
              </CFormLabel>
              <CFormSelect
                id="audit-filter-status"
                size="sm"
                value={statusFilter}
                onChange={(event) => onStatusFilterChange(event.target.value)}
              >
                <option value="">{t("audit.filters.all")}</option>
                {statusOptions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </CFormSelect>
            </div>

            <div className="audit-filter-item">
              <CFormLabel
                htmlFor="audit-filter-method"
                className="mb-1 text-body-secondary"
              >
                {t("audit.filters.method")}
              </CFormLabel>
              <CFormSelect
                id="audit-filter-method"
                size="sm"
                value={methodFilter}
                onChange={(event) => onMethodFilterChange(event.target.value)}
              >
                <option value="">{t("audit.filters.all")}</option>
                {methodOptions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </CFormSelect>
            </div>

            <div className="audit-filter-item">
              <CFormLabel
                htmlFor="audit-filter-action"
                className="mb-1 text-body-secondary"
              >
                {t("audit.filters.action")}
              </CFormLabel>
              <CFormSelect
                id="audit-filter-action"
                size="sm"
                value={actionFilter}
                onChange={(event) => onActionFilterChange(event.target.value)}
              >
                <option value="">{t("audit.filters.all")}</option>
                {actionOptions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </CFormSelect>
            </div>

            <CButton
              color="light"
              size="sm"
              onClick={onClearFilters}
              className="audit-filter-item__clear"
            >
              {t("audit.filters.clear")}
            </CButton>
          </div>
        </div>
      )}
    </div>
  );
}
