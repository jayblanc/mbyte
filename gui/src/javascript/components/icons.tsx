import { CIcon } from "@coreui/icons-react";
import { cilHome, cilList, cilStorage } from "@coreui/icons";

export type IconSize = "sm" | "lg" | "xl";

export function DashboardIcon({ size }: { size?: IconSize } = {}) {
  return <CIcon icon={cilHome} size={size} />;
}

export function StoreIcon({ size }: { size?: IconSize } = {}) {
  return <CIcon icon={cilStorage} size={size} />;
}

export function AuditIcon({ size }: { size?: IconSize } = {}) {
  return <CIcon icon={cilList} size={size} />;
}
