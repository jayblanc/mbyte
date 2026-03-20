import {CNavItem} from '@coreui/react'
import type {PropsWithChildren, ReactNode} from 'react'
import {NavLink} from 'react-router-dom'

export type SidebarNavItemProps = PropsWithChildren<{
    to: string
    icon?: ReactNode
    compact?: boolean
    to2: string
    icon2?: ReactNode
}>

/**
 * Sidebar nav item that uses React Router for SPA navigation,
 * while keeping CoreUI sidebar/nav styling.
 */
export function SidebarStoreNavItem({to, icon, compact, children, to2, icon2}: SidebarNavItemProps) {
    return (
        <CNavItem>
            <div className="mbyte-sidebar-link__inner">
                <NavLink
                    to={to}
                    className={({isActive}) =>
                        `mbyte-sidebar-link ${compact ? 'is-compact' : ''} ${isActive ? 'is-active' : ''}`.trim()
                    }
                    style={{cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px'}}
                >
                    {icon && <span className="mbyte-sidebar-link__icon">{icon}</span>}
                    {!compact && <span className="mbyte-sidebar-link__label">{children}</span>}
                </NavLink>
                {icon2 && (
                    <NavLink
                        to={to2}
                        style={{cursor: 'pointer'}}
                    >
                        <span className="mbyte-sidebar-link__icon">{icon2}</span>
                    </NavLink>
                )}
            </div>
        </CNavItem>
    )
}

