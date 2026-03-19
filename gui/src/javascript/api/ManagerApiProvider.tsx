import { createContext, type PropsWithChildren, useContext, useMemo } from 'react'
import { createManagerApi } from './managerApi'
import type { Profile } from './entities/Profile'
import type { TokenProvider } from './fetchWithAuth'
import { useAccessToken } from '../auth/useAccessToken'
import type { Application } from './entities/Application'
import type {Process} from "./entities/Process.ts";
import type { CommandDescriptor } from './entities/CommandDescriptor'

export type ManagerApi = {
  getHealth(): Promise<unknown>
  getCurrentProfile(): Promise<Profile>
  // manager API: apps management
  listApps(owner?: string): Promise<Application[]>
  getApp(appId: string): Promise<Application>
  createApp(type: string, name: string): Promise<string>
  deleteApp(appId: string): Promise<void>
  getAppProcs(appId: string, active: boolean): Promise<Process[]>
  listAppCommands(appId: string): Promise<CommandDescriptor[]>
  runAppCommand(appId: string, commandName: string): Promise<string>
  getAppProc(appId: string, procId: string): Promise<Process>
  getStatus(): Promise<import('./entities/ManagerStatus').ManagerStatus>
}

const ManagerApiContext = createContext<ManagerApi | null>(null)

export type ManagerApiProviderProps = {
  /**
   * Optional override for the token provider.
   * By default, the provider uses the OIDC access token from react-oidc-context.
   */
  tokenProvider?: TokenProvider
}

export function ManagerApiProvider({ children, tokenProvider }: PropsWithChildren<ManagerApiProviderProps>) {
  const defaultTokenProvider = useAccessToken()

  const api = useMemo(() => {
    return createManagerApi(tokenProvider ?? defaultTokenProvider)
  }, [tokenProvider, defaultTokenProvider])

  return <ManagerApiContext.Provider value={api}>{children}</ManagerApiContext.Provider>
}

export function useManagerApi(): ManagerApi {
  const api = useContext(ManagerApiContext)
  if (!api) {
    throw new Error('useManagerApi must be used within a ManagerApiProvider')
  }
  return api
}
