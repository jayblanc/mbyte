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

import { CButton } from "@coreui/react";
import { useTranslation } from "react-i18next";

type AuditPaginationProps = {
  page: number;
  totalPages: number;
  totalItems: number;
  pageSize: number;
  onPageChange: (page: number) => void;
};

export function AuditPagination({
  page,
  totalPages,
  totalItems,
  pageSize,
  onPageChange,
}: AuditPaginationProps) {
  const { t } = useTranslation();
  const first = totalItems === 0 ? 0 : (page - 1) * pageSize + 1;
  const last = Math.min(page * pageSize, totalItems);

  return (
    <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 pt-3">
      <small className="text-body-secondary">
        {t("audit.pagination.summary", { first, last, total: totalItems })}
      </small>

      <div className="d-flex align-items-center gap-2">
        <CButton
          color="light"
          size="sm"
          disabled={page <= 1}
          onClick={() => onPageChange(page - 1)}
        >
          {t("audit.pagination.previous")}
        </CButton>

        <small className="text-body-secondary">
          {t("audit.pagination.page", { page, totalPages })}
        </small>

        <CButton
          color="light"
          size="sm"
          disabled={page >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          {t("audit.pagination.next")}
        </CButton>
      </div>
    </div>
  );
}