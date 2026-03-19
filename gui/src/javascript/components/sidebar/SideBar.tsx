import { CSidebar, CSidebarHeader, CSidebarNav } from '@coreui/react'
import { DashboardIcon, SidebarBrand, SidebarNavItem, SidebarProfile, StoreIcon } from '../index'
import { useManagerStatus } from '../../auth/useManagerStatus'
import { useEffect } from 'react'

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

          <SidebarNavItem to="/webhooks" icon={<DashboardIcon size={narrow ? 'lg' : undefined} />} compact={narrow}>
              Webhooks
          </SidebarNavItem>

        {hasStore && (
          <SidebarNavItem to="/s/0/" icon={<StoreIcon size={narrow ? 'lg' : undefined} />} compact={narrow}>
            Store
          </SidebarNavItem>
        )}
      </CSidebarNav>
    </CSidebar>
  )
}
