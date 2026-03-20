import { CSidebar, CSidebarHeader, CSidebarNav } from '@coreui/react'
import {DashboardIcon, ManageIcon, SidebarBrand, SidebarNavItem, SidebarProfile, StoreIcon} from '../index'
import { useManagerStatus } from '../../auth/useManagerStatus'
import { useEffect } from 'react'
import {SidebarStoreNavItem} from "./SidebarStoreNavItem.tsx";

export type SideBarProps = {
  narrow: boolean
}

export function SideBar({ narrow }: SideBarProps) {
  const { hasStore, reload: reloadStatus } = useManagerStatus()

  // Listen for app creation event from WebSocket
  useEffect(() => {
    const handleAppCreated = () => {
      reloadStatus()
    }
    globalThis.addEventListener('app-created', handleAppCreated)
    return () => globalThis.removeEventListener('app-created', handleAppCreated)
  }, [reloadStatus])

  return (
    <CSidebar narrow={narrow} className="mbyte-sidebar">
      <CSidebarHeader className="mbyte-header">
        <SidebarBrand compact={narrow} />
      </CSidebarHeader>

      <div className="border-bottom p-3">
        <SidebarProfile compact={narrow} />
      </div>

      <CSidebarNav>
        <SidebarNavItem to="/dashboard" icon={<DashboardIcon size={narrow ? 'lg' : undefined} />} compact={narrow}>
          Dashboard
        </SidebarNavItem>

        {hasStore && (
          /*<SidebarNavItem to="/s/0/" icon={<StoreIcon size={narrow ? 'lg' : undefined} />} compact={narrow}>
            Store
          </SidebarNavItem>*/
            <SidebarStoreNavItem to="/s/0/" icon={<StoreIcon size={narrow ? 'lg' : undefined} />} compact={narrow} to2="/m/0" icon2={<ManageIcon size={narrow ? 'lg': undefined}/>}>
        Store
    </SidebarStoreNavItem>
        )}
      </CSidebarNav>
    </CSidebar>
  )
}
