import { CSidebar, CSidebarHeader, CSidebarNav } from "@coreui/react";
import {
  AuditIcon,
  DashboardIcon,
  SidebarBrand,
  SidebarNavItem,
  SidebarProfile,
  StoreIcon,
} from "../index";
import { useManagerStatus } from "../../auth/useManagerStatus";
import { useEffect } from "react";

export type SideBarProps = {
  narrow: boolean;
};

export function SideBar({ narrow }: SideBarProps) {
  const { hasStore, reload: reloadStatus } = useManagerStatus();

  // Listen for app creation event from WebSocket
  useEffect(() => {
    const handleAppCreated = () => {
      reloadStatus();
    };
    globalThis.addEventListener("app-created", handleAppCreated);
    return () =>
      globalThis.removeEventListener("app-created", handleAppCreated);
  }, [reloadStatus]);

  return (
    <CSidebar narrow={narrow} className="mbyte-sidebar">
      <CSidebarHeader className="mbyte-header">
        <SidebarBrand compact={narrow} />
      </CSidebarHeader>

      <div className="border-bottom p-3">
        <SidebarProfile compact={narrow} />
      </div>

      <CSidebarNav>
        <SidebarNavItem
          to="/dashboard"
          icon={<DashboardIcon size={narrow ? "lg" : undefined} />}
          compact={narrow}
        >
          Dashboard
        </SidebarNavItem>

        {hasStore && (
          <>
            <SidebarNavItem
              to="/s/0/"
              icon={<StoreIcon size={narrow ? "lg" : undefined} />}
              compact={narrow}
            >
              Store
            </SidebarNavItem>

            <SidebarNavItem
              to="/audits"
              icon={<AuditIcon size={narrow ? "lg" : undefined} />}
              compact={narrow}
            >
              Audit
            </SidebarNavItem>
          </>
        )}
      </CSidebarNav>
    </CSidebar>
  );
}
