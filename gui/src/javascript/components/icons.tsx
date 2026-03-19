import { CIcon } from '@coreui/icons-react'
import {cilHome, cilSettings, cilStorage} from '@coreui/icons'

export type IconSize = 'sm' | 'lg' | 'xl'

export function DashboardIcon({ size }: { size?: IconSize } = {}) {
  return <CIcon icon={cilHome} size={size} />
}

export function StoreIcon({ size }: { size?: IconSize } = {}) {
  return <CIcon icon={cilStorage} size={size} />
}

export function ManageIcon({ size }: { size?: IconSize } = {}) {
    return <CIcon icon={cilSettings} size={size} />
}
